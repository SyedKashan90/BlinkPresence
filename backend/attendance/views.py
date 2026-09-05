from rest_framework import status, viewsets
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from rest_framework.views import APIView

from audit_logs.utils import log_action
from authentication.permissions import IsAdminOrTeacher, IsStudent
from students.models import Student
from users.models import User
from .models import Attendance
from .serializers import AttendanceSerializer, MarkAttendanceSerializer
from .services import AttendanceValidationError, mark_attendance


class MarkAttendanceView(APIView):
    """POST /api/attendance — FR-8 / UC-3. The full validation chain lives
    in attendance.services.mark_attendance; this view only wires the
    authenticated student to it and translates failures to 4xx responses."""

    permission_classes = [IsStudent]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "attendance"

    def post(self, request):
        serializer = MarkAttendanceSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        student = Student.objects.get(user=request.user)

        try:
            attendance = mark_attendance(
                student=student,
                token=serializer.validated_data["session_id"],
                verification_method=serializer.validated_data["verification_method"],
                scanned_at=serializer.validated_data.get("scanned_at"),
            )
        except AttendanceValidationError as exc:
            return Response({"status": "failed", "detail": str(exc)}, status=status.HTTP_400_BAD_REQUEST)

        log_action(
            user=request.user, request=request, action="mark_attendance", entity_type="Attendance", entity_id=attendance.id
        )
        return Response({"status": "success", "attendance": AttendanceSerializer(attendance).data}, status=status.HTTP_201_CREATED)


class AttendanceViewSet(viewsets.ReadOnlyModelViewSet):
    """Read access to the attendance ledger: admins/teachers see everyone
    (scoped to their own courses for teachers); students see only their own
    history (FR-2 / attendance history screen)."""

    serializer_class = AttendanceSerializer
    permission_classes = [IsAdminOrTeacher | IsStudent]
    filterset_fields = ["lecture", "student", "lecture__course", "attendance_status"]

    def get_queryset(self):
        user = self.request.user
        qs = Attendance.objects.select_related("student", "lecture", "lecture__course")
        if user.role == User.Role.STUDENT:
            return qs.filter(student__user=user)
        if user.role == User.Role.TEACHER:
            return qs.filter(lecture__teacher__user=user)
        return qs


class MyAttendanceView(APIView):
    """GET /api/attendance/student/{id} per the TRD — a student's own
    attendance history (FR-2), or any student's history if requested by
    an admin/teacher."""

    permission_classes = [IsAdminOrTeacher | IsStudent]

    def get(self, request, student_id):
        if request.user.role == User.Role.STUDENT:
            student = Student.objects.get(user=request.user)
            if str(student.id) != str(student_id):
                return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)
        records = Attendance.objects.select_related("lecture", "lecture__course").filter(student_id=student_id)
        return Response(AttendanceSerializer(records, many=True).data)
