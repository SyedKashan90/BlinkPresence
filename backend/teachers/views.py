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
