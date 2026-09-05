from django.urls import path
from rest_framework.routers import DefaultRouter

from .views import AttendanceViewSet, MarkAttendanceView, MyAttendanceView

router = DefaultRouter()
router.register("attendance-records", AttendanceViewSet, basename="attendance-record")

urlpatterns = [
    path("attendance", MarkAttendanceView.as_view(), name="attendance-mark"),
    path("attendance/student/<uuid:student_id>", MyAttendanceView.as_view(), name="attendance-by-student"),
    *router.urls,
]
