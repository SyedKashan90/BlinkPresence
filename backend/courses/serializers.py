from rest_framework import serializers

from departments.serializers import DepartmentSerializer
from teachers.serializers import TeacherSerializer
from .models import Course, Enrollment, Lecture


class CourseSerializer(serializers.ModelSerializer):
    department_detail = DepartmentSerializer(source="department", read_only=True)
    teacher_detail = TeacherSerializer(source="teacher", read_only=True)

    class Meta:
        model = Course
        fields = [
            "id", "course_code", "course_name", "credit_hours", "department", "department_detail",
            "teacher", "teacher_detail", "semester", "created_at",
        ]
        read_only_fields = ["id", "created_at"]


class EnrollmentSerializer(serializers.ModelSerializer):
    student_name = serializers.CharField(source="student.full_name", read_only=True)
    registration_number = serializers.CharField(source="student.registration_number", read_only=True)
    course_code = serializers.CharField(source="course.course_code", read_only=True)

    class Meta:
        model = Enrollment
        fields = ["id", "student", "student_name", "registration_number", "course", "course_code", "enrolled_at"]
        read_only_fields = ["id", "enrolled_at"]
        validators = []  # uniqueness is enforced explicitly in validate() for a clearer error message

    def validate(self, attrs):
        student = attrs.get("student", getattr(self.instance, "student", None))
        course = attrs.get("course", getattr(self.instance, "course", None))
        qs = Enrollment.objects.filter(student=student, course=course)
        if self.instance:
            qs = qs.exclude(pk=self.instance.pk)
        if qs.exists():
            raise serializers.ValidationError("This student is already enrolled in this course.")
        return attrs


class LectureSerializer(serializers.ModelSerializer):
    course_code = serializers.CharField(source="course.course_code", read_only=True)
    course_name = serializers.CharField(source="course.course_name", read_only=True)

    class Meta:
        model = Lecture
        fields = [
            "id", "course", "course_code", "course_name", "teacher", "lecture_title",
            "lecture_date", "start_time", "end_time", "status", "created_at",
        ]
        read_only_fields = ["id", "status", "created_at"]
