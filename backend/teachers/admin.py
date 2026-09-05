from django.contrib import admin

from .models import Teacher


@admin.register(Teacher)
class TeacherAdmin(admin.ModelAdmin):
    list_display = ["employee_id", "full_name", "department", "designation"]
    list_filter = ["department"]
    search_fields = ["full_name", "employee_id", "user__email"]
