import calendar
from datetime import date, datetime, timedelta

from django.http import HttpResponse
from django.shortcuts import get_object_or_404
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.views import APIView

from attendance.models import Attendance
from authentication.permissions import IsAdminOrTeacher
from courses.models import Course
from students.models import Student
from teachers.models import Teacher
from users.models import User
from .services import attendance_queryset_to_rows, build_excel_report, build_pdf_report

CONTENT_TYPES = {
    "pdf": "application/pdf",
    "excel": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
}


def _respond(request, title: str, queryset):
    # NOTE: the query param is named "export", not "format" — DRF reserves
    # ?format= for its own content-negotiation and 404s on unknown values.
    fmt = request.query_params.get("export", "pdf").lower()
    if fmt not in CONTENT_TYPES:
        raise ValidationError({"export": "Must be 'pdf' or 'excel'."})

    rows = attendance_queryset_to_rows(queryset)
    body = build_pdf_report(title, rows) if fmt == "pdf" else build_excel_report(title, rows)
    extension = "pdf" if fmt == "pdf" else "xlsx"

    response = HttpResponse(body, content_type=CONTENT_TYPES[fmt])
    filename = f"{title.replace(' ', '_')}.{extension}"
    response["Content-Disposition"] = f'attachment; filename="{filename}"'
    return response


def _teacher_courses(user):
    teacher = Teacher.objects.get(user=user)
    return Course.objects.filter(teacher=teacher)


class CourseReportView(APIView):
    """GET /api/reports/course/{id} — FR-9 course report."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request, course_id):
        course = get_object_or_404(Course, id=course_id)
        if request.user.role == User.Role.TEACHER and course.teacher_id and course.teacher.user_id != request.user.id:
            raise PermissionDenied("You do not teach this course.")

        queryset = Attendance.objects.filter(lecture__course=course)
        return _respond(request, f"{course.course_code} Attendance Report", queryset)


class StudentReportView(APIView):
    """GET /api/reports/student/{id} — FR-9 student report."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request, student_id):
        student = get_object_or_404(Student, id=student_id)
        if request.user.role == User.Role.TEACHER:
            if not student.enrollments.filter(course__in=_teacher_courses(request.user)).exists():
                raise PermissionDenied("You do not teach this student.")

        queryset = Attendance.objects.filter(student=student)
        return _respond(request, f"{student.registration_number} Attendance Report", queryset)


class DailyReportView(APIView):
    """GET /api/reports/daily?date=YYYY-MM-DD — FR-9 daily report."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request):
        day = self._parse_date(request.query_params.get("date")) or date.today()
        queryset = Attendance.objects.filter(lecture__lecture_date=day)
        if request.user.role == User.Role.TEACHER:
            queryset = queryset.filter(lecture__course__in=_teacher_courses(request.user))
        return _respond(request, f"Daily Attendance Report {day.isoformat()}", queryset)

    @staticmethod
    def _parse_date(value):
        if not value:
            return None
        try:
            return datetime.strptime(value, "%Y-%m-%d").date()
        except ValueError as exc:
            raise ValidationError({"date": "Expected format YYYY-MM-DD."}) from exc


class WeeklyReportView(APIView):
    """GET /api/reports/weekly?week_start=YYYY-MM-DD — FR-9 weekly report."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request):
        start = DailyReportView._parse_date(request.query_params.get("week_start"))
        if start is None:
            today = date.today()
            start = today - timedelta(days=today.weekday())
        end = start + timedelta(days=6)

        queryset = Attendance.objects.filter(lecture__lecture_date__range=(start, end))
        if request.user.role == User.Role.TEACHER:
            queryset = queryset.filter(lecture__course__in=_teacher_courses(request.user))
        return _respond(request, f"Weekly Attendance Report {start.isoformat()}_{end.isoformat()}", queryset)


class MonthlyReportView(APIView):
    """GET /api/reports/monthly?year=&month= — FR-9 monthly report."""

    permission_classes = [IsAdminOrTeacher]

    def get(self, request):
        today = date.today()
        try:
            year = int(request.query_params.get("year", today.year))
            month = int(request.query_params.get("month", today.month))
        except ValueError as exc:
            raise ValidationError({"year|month": "Must be integers."}) from exc

        last_day = calendar.monthrange(year, month)[1]
        start, end = date(year, month, 1), date(year, month, last_day)

        queryset = Attendance.objects.filter(lecture__lecture_date__range=(start, end))
        if request.user.role == User.Role.TEACHER:
            queryset = queryset.filter(lecture__course__in=_teacher_courses(request.user))
        return _respond(request, f"Monthly Attendance Report {year}-{month:02d}", queryset)
