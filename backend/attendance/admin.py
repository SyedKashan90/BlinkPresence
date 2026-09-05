from django.contrib import admin

from .models import Attendance


@admin.register(Attendance)
class AttendanceAdmin(admin.ModelAdmin):
    list_display = ["student", "lecture", "attendance_status", "verification_method", "marked_at"]
    list_filter = ["attendance_status", "verification_method"]
    search_fields = ["student__registration_number", "lecture__course__course_code"]
