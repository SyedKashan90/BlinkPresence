from django.contrib import admin

from .models import RegisteredDevice, Student


@admin.register(Student)
class StudentAdmin(admin.ModelAdmin):
    list_display = ["registration_number", "full_name", "department", "semester", "batch"]
    list_filter = ["department", "semester"]
    search_fields = ["full_name", "registration_number", "user__email"]


@admin.register(RegisteredDevice)
class RegisteredDeviceAdmin(admin.ModelAdmin):
    list_display = ["student", "device_id", "device_name", "last_used"]
    search_fields = ["device_id", "student__registration_number"]
