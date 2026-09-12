from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from audit_logs.utils import log_action
from authentication.permissions import IsAdmin, IsTeacher
from .models import Teacher
from .serializers import TeacherCreateSerializer, TeacherSerializer


class TeacherViewSet(viewsets.ModelViewSet):
    """Admin manages the teacher roster (UC-7); a teacher may view their own
    profile via the `me` action."""

    queryset = Teacher.objects.select_related("user", "department").all()
    filterset_fields = ["department"]
    search_fields = ["full_name", "employee_id"]

    def get_serializer_class(self):
        if self.action == "create":
            return TeacherCreateSerializer
        return TeacherSerializer

    def get_permissions(self):
        if self.action == "me":
            return [IsTeacher()]
        return [IsAdmin()]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        teacher = serializer.save()
        log_action(user=request.user, request=request, action="create_teacher", entity_type="Teacher", entity_id=teacher.id)
        # Respond with the full read serializer (includes id, department_detail, ...)
        # rather than the write-only create serializer's limited field set.
        return Response(TeacherSerializer(teacher).data, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["get"])
    def me(self, request):
        teacher = Teacher.objects.select_related("user", "department").get(user=request.user)
        return Response(TeacherSerializer(teacher).data)

    @action(detail=True, methods=["post"], url_path="reset-password")
    def reset_password(self, request, pk=None):
        teacher = self.get_object()
        new_password = request.data.get("new_password")
        if not new_password or len(new_password) < 8:
            return Response(
                {"new_password": ["Password must be at least 8 characters long."]},
                status=status.HTTP_400_BAD_REQUEST,
            )
        teacher.user.set_password(new_password)
        teacher.user.save(update_fields=["password"])
        log_action(
            user=request.user,
            request=request,
            action="admin_reset_teacher_password",
            entity_type="Teacher",
            entity_id=teacher.id,
        )
        return Response({"detail": f"Password reset successfully for teacher {teacher.full_name}."})
