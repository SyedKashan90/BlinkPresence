from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from audit_logs.utils import log_action
from authentication.permissions import IsAdmin, IsStudent, IsTeacher
from .models import RegisteredDevice, Student
from .serializers import RegisteredDeviceSerializer, StudentCreateSerializer, StudentSelfUpdateSerializer, StudentSerializer


class StudentViewSet(viewsets.ModelViewSet):
    """Admin manages the roster (UC-7); a student may view/update their own
    profile (FR-2) via the `me` action."""

    queryset = Student.objects.select_related("user", "department").all()
    filterset_fields = ["department", "semester", "batch"]
    search_fields = ["full_name", "registration_number"]

    def get_serializer_class(self):
        if self.action == "create":
            return StudentCreateSerializer
        if self.action in ("update_me",):
            return StudentSelfUpdateSerializer
        return StudentSerializer

    def get_permissions(self):
        # Self-service: a student only ever sees/edits their own profile via
        # the /me actions below, never via list/retrieve-by-id.
        if self.action in ("me", "update_me", "my_devices"):
            return [IsStudent()]
        # Roster management is admin-only; teachers may look up individual
        # students (e.g. while reviewing a course roster) but not enumerate
        # or edit the whole student body.
        if self.action == "retrieve":
            return [(IsAdmin | IsTeacher)()]
        return [IsAdmin()]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        student = serializer.save()
        log_action(user=request.user, request=request, action="create_student", entity_type="Student", entity_id=student.id)
        # Respond with the full read serializer (includes id, department_detail, ...)
        # rather than the write-only create serializer's limited field set.
        return Response(StudentSerializer(student).data, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=["get"])
    def me(self, request):
        student = Student.objects.select_related("user", "department").get(user=request.user)
        return Response(StudentSerializer(student).data)

    @action(detail=False, methods=["patch"], url_path="me/update")
    def update_me(self, request):
        student = Student.objects.get(user=request.user)
        serializer = self.get_serializer(student, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(StudentSerializer(student).data)

    @action(detail=False, methods=["get", "post"], url_path="me/devices")
    def my_devices(self, request):
        student = Student.objects.get(user=request.user)
        if request.method == "POST":
            serializer = RegisteredDeviceSerializer(data=request.data)
            serializer.is_valid(raise_exception=True)
            device = serializer.save(student=student)
            log_action(user=request.user, request=request, action="register_device", entity_type="RegisteredDevice", entity_id=device.id)
            return Response(RegisteredDeviceSerializer(device).data, status=status.HTTP_201_CREATED)
        devices = student.registered_devices.all()
        return Response(RegisteredDeviceSerializer(devices, many=True).data)

    @action(detail=True, methods=["post"], url_path="reset-password")
    def reset_password(self, request, pk=None):
        student = self.get_object()
        new_password = request.data.get("new_password")
        if not new_password or len(new_password) < 8:
            return Response(
                {"new_password": ["Password must be at least 8 characters long."]},
                status=status.HTTP_400_BAD_REQUEST,
            )
        student.user.set_password(new_password)
        student.user.save(update_fields=["password"])
        log_action(
            user=request.user,
            request=request,
            action="admin_reset_student_password",
            entity_type="Student",
            entity_id=student.id,
        )
        return Response({"detail": f"Password reset successfully for student {student.full_name}."})
