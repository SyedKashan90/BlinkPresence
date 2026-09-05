from rest_framework import serializers

from .models import Attendance


class MarkAttendanceSerializer(serializers.Serializer):
    """POST /api/attendance body — matches the TRD's Mark Attendance
    payload (student_id is implicit: the authenticated student)."""

    session_id = serializers.CharField(help_text="The signed QR token scanned by the student, not just the UUID.")
    verification_method = serializers.ChoiceField(
        choices=Attendance.VerificationMethod.choices, default=Attendance.VerificationMethod.FACE_RECOGNITION
    )
    scanned_at = serializers.DateTimeField(
        required=False, help_text="Client-side timestamp of a successful on-device verification; used for offline-sync grace-window validation."
    )


class AttendanceSerializer(serializers.ModelSerializer):
    student_name = serializers.CharField(source="student.full_name", read_only=True)
    registration_number = serializers.CharField(source="student.registration_number", read_only=True)
    course_code = serializers.CharField(source="lecture.course.course_code", read_only=True)
    lecture_date = serializers.DateField(source="lecture.lecture_date", read_only=True)

    class Meta:
        model = Attendance
        fields = [
            "id", "student", "student_name", "registration_number", "lecture", "course_code", "lecture_date",
            "session", "attendance_status", "verification_method", "marked_at", "created_at",
        ]
        read_only_fields = fields
