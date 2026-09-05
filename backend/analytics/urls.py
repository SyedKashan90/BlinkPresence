from django.urls import path

from .views import (
    CourseAnalyticsView,
    DashboardSummaryView,
    DefaulterListView,
    DepartmentAnalyticsView,
    StudentStatisticsView,
)

urlpatterns = [
    path("analytics/dashboard", DashboardSummaryView.as_view(), name="analytics-dashboard"),
    path("analytics/defaulters", DefaulterListView.as_view(), name="analytics-defaulters"),
    path("analytics/student/<uuid:student_id>", StudentStatisticsView.as_view(), name="analytics-student"),
    path("analytics/course/<uuid:course_id>", CourseAnalyticsView.as_view(), name="analytics-course"),
    path("analytics/department/<uuid:department_id>", DepartmentAnalyticsView.as_view(), name="analytics-department"),
]
