from rest_framework import viewsets

from authentication.permissions import IsAdmin, IsAdminOrTeacher
from teachers.models import Teacher
from users.models import User
from .models import Course, Enrollment, Lecture
from .serializers import CourseSerializer, EnrollmentSerializer, LectureSerializer


class CourseViewSet(viewsets.ModelViewSet):
    """Admin manages the course catalogue (UC-8); teachers get read access
    plus visibility into the courses assigned to them."""

    queryset = Course.objects.select_related("department", "teacher", "teacher__user").all()
    serializer_class = CourseSerializer
    filterset_fields = ["department", "teacher", "semester"]
    search_fields = ["course_code", "course_name"]

    def get_permissions(self):
        if self.action in ("list", "retrieve"):
            return [IsAdminOrTeacher()]
        return [IsAdmin()]

    def get_queryset(self):
        qs = super().get_queryset()
        user = self.request.user
        if user.role == User.Role.TEACHER:
            return qs.filter(teacher__user=user)
        return qs


class EnrollmentViewSet(viewsets.ModelViewSet):
    """Admin-only: assigns students to courses (part of UC-8)."""

    queryset = Enrollment.objects.select_related("student", "course").all()
    serializer_class = EnrollmentSerializer
    permission_classes = [IsAdmin]
    filterset_fields = ["student", "course"]


class LectureViewSet(viewsets.ModelViewSet):
    """Teachers schedule/manage lectures for their own courses; admins have
    full visibility. QR-session lifecycle (start/end) lives in qr_sessions."""

    queryset = Lecture.objects.select_related("course", "teacher", "teacher__user").all()
    serializer_class = LectureSerializer
    permission_classes = [IsAdminOrTeacher]
    filterset_fields = ["course", "teacher", "status", "lecture_date"]

    def get_queryset(self):
        qs = super().get_queryset()
        user = self.request.user
        if user.role == User.Role.TEACHER:
            return qs.filter(teacher__user=user)
        return qs

    def perform_create(self, serializer):
        user = self.request.user
        if user.role == User.Role.TEACHER:
            teacher = Teacher.objects.get(user=user)
            serializer.save(teacher=teacher)
        else:
            serializer.save()
