from rest_framework import serializers

from .models import AttendanceStatistics


class AttendanceStatisticsSerializer(serializers.ModelSerializer):
    course_code = serializers.CharField(source="course.course_code", read_only=True)
    course_name = serializers.CharField(source="course.course_name", read_only=True)
    student_name = serializers.CharField(source="student.full_name", read_only=True)
    registration_number = serializers.CharField(source="student.registration_number", read_only=True)

    class Meta:
        model = AttendanceStatistics
        fields = [
            "id", "student", "student_name", "registration_number", "course", "course_code", "course_name",
            "total_lectures", "attended_lectures", "attendance_percentage", "updated_at",
        ]
        read_only_fields = fields
