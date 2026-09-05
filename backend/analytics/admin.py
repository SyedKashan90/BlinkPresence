from django.contrib import admin

from .models import AttendanceStatistics


@admin.register(AttendanceStatistics)
class AttendanceStatisticsAdmin(admin.ModelAdmin):
    list_display = ["student", "course", "total_lectures", "attended_lectures", "attendance_percentage"]
    list_filter = ["course"]
    search_fields = ["student__registration_number", "course__course_code"]
