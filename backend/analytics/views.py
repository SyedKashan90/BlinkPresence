from django.db.models import Avg, Count
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import PermissionDenied
from rest_framework.response import Response
from rest_framework.views import APIView

from authentication.permissions import IsAdmin, IsAdminOrTeacher, IsStudent
from courses.models import Course
from departments.models import Department
from students.models import Student
from teachers.models import Teacher
from users.models import User
from .models import AttendanceStatistics
from .serializers import AttendanceStatisticsSerializer

DEFAULT_DEFAULTER_THRESHOLD = 75


def _course_visible_to(user, course: Course) -> bool:
    if user.role == User.Role.ADMIN:
        return True
    if user.role == User.Role.TEACHER:
        return course.teacher_id is not None and course.teacher.user_id == user.id
    return False


class StudentStatisticsView(APIView):
    """GET /api/analytics/student/{id} — FR-10 attendance percentage,
    per-course breakdown. Students may only view their own."""

    permission_classes = [IsAdminOrTeacher | IsStudent]

    def get(self, request, student_id):
        student = get_object_or_404(Student, id=student_id)
        if request.user.role == User.Role.STUDENT and student.user_id != request.user.id:
            raise PermissionDenied("You may only view your own analytics.")
        if request.user.role == User.Role.TEACHER:
            teacher = Teacher.objects.get(user=request.user)
            if not student.enrollments.filter(course__teacher=teacher).exists():
                raise PermissionDenied("You do not teach this student.")

        stats = AttendanceStatistics.objects.filter(student=student).select_related("course")
        overall = stats.aggregate(avg=Avg("attendance_percentage"))["avg"] or 0
        return Response(
            {
                "student_id": student.id,
                "registration_number": student.registration_number,
                "overall_attendance_percentage": round(overall, 2),
                "courses": AttendanceStatisticsSerializer(stats, many=True).data,
            }
        )


class CourseAnalyticsView(APIView):
    """GET /api/analytics/course/{id} — per-student breakdown + course
    average for the Attendance Report screen and Admin Analytics Dashboard."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request, course_id):
        course = get_object_or_404(Course, id=course_id)
        if not _course_visible_to(request.user, course):
            raise PermissionDenied("You do not teach this course.")

        stats = AttendanceStatistics.objects.filter(course=course).select_related("student")
        summary = stats.aggregate(avg=Avg("attendance_percentage"), students=Count("id"))

        return Response(
            {
                "course_id": course.id,
                "course_code": course.course_code,
                "average_attendance_percentage": round(summary["avg"] or 0, 2),
                "enrolled_students": summary["students"],
                "students": AttendanceStatisticsSerializer(stats, many=True).data,
            }
        )


class DepartmentAnalyticsView(APIView):
    """GET /api/analytics/department/{id} — admin-only institutional view
    (Admin Analytics: department comparison)."""

    permission_classes = [IsAdmin]

    def get(self, request, department_id):
        department = get_object_or_404(Department, id=department_id)
        courses = Course.objects.filter(department=department)
        stats = AttendanceStatistics.objects.filter(course__in=courses)
        summary = stats.aggregate(avg=Avg("attendance_percentage"))

        per_course = (
            stats.values("course__id", "course__course_code", "course__course_name")
            .annotate(avg_attendance=Avg("attendance_percentage"), student_count=Count("id"))
            .order_by("course__course_code")
        )

        return Response(
            {
                "department_id": department.id,
                "department_name": department.department_name,
                "average_attendance_percentage": round(summary["avg"] or 0, 2),
                "courses": list(per_course),
            }
        )


class DefaulterListView(APIView):
    """GET /api/analytics/defaulters?course=&threshold= — FR-10 defaulter
    analysis: students below the attendance threshold (default 75%)."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request):
        threshold = float(request.query_params.get("threshold", DEFAULT_DEFAULTER_THRESHOLD))
        qs = AttendanceStatistics.objects.select_related("student", "course").filter(
            attendance_percentage__lt=threshold
        )

        course_id = request.query_params.get("course")
        if course_id:
            course = get_object_or_404(Course, id=course_id)
            if not _course_visible_to(request.user, course):
                raise PermissionDenied("You do not teach this course.")
            qs = qs.filter(course=course)
        elif request.user.role == User.Role.TEACHER:
            teacher = Teacher.objects.get(user=request.user)
            qs = qs.filter(course__teacher=teacher)

        return Response({"threshold": threshold, "defaulters": AttendanceStatisticsSerializer(qs, many=True).data})


class DashboardSummaryView(APIView):
    """GET /api/analytics/dashboard — role-scoped real-time widgets
    (Admin Home / Teacher Dashboard widgets)."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request):
        user = request.user
        if user.role == User.Role.ADMIN:
            return Response(
                {
                    "total_students": Student.objects.count(),
                    "total_teachers": Teacher.objects.count(),
                    "total_departments": Department.objects.count(),
                    "total_courses": Course.objects.count(),
                    "overall_attendance_percentage": round(
                        AttendanceStatistics.objects.aggregate(avg=Avg("attendance_percentage"))["avg"] or 0, 2
                    ),
                    "active_sessions": Course.objects.filter(lectures__status="active").distinct().count(),
                }
            )

        teacher = Teacher.objects.get(user=user)
        courses = Course.objects.filter(teacher=teacher)
        return Response(
            {
                "active_courses": courses.count(),
                "todays_lectures": teacher.lectures.filter(lecture_date=timezone.now().date()).count(),
                "attendance_summary": round(
                    AttendanceStatistics.objects.filter(course__in=courses).aggregate(avg=Avg("attendance_percentage"))["avg"] or 0, 2
                ),
            }
        )
