from django.urls import path

from .views import CourseReportView, DailyReportView, MonthlyReportView, StudentReportView, WeeklyReportView

urlpatterns = [
    path("reports/daily", DailyReportView.as_view(), name="report-daily"),
    path("reports/weekly", WeeklyReportView.as_view(), name="report-weekly"),
    path("reports/monthly", MonthlyReportView.as_view(), name="report-monthly"),
    path("reports/course/<uuid:course_id>", CourseReportView.as_view(), name="report-course"),
    path("reports/student/<uuid:student_id>", StudentReportView.as_view(), name="report-student"),
]
