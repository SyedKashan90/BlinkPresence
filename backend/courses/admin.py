from django.contrib import admin

from .models import Course, Enrollment, Lecture


@admin.register(Course)
class CourseAdmin(admin.ModelAdmin):
    list_display = ["course_code", "course_name", "department", "teacher", "semester"]
    list_filter = ["department", "semester"]
    search_fields = ["course_code", "course_name"]


@admin.register(Enrollment)
class EnrollmentAdmin(admin.ModelAdmin):
    list_display = ["student", "course", "enrolled_at"]
    search_fields = ["student__registration_number", "course__course_code"]


@admin.register(Lecture)
class LectureAdmin(admin.ModelAdmin):
    list_display = ["course", "teacher", "lecture_date", "start_time", "status"]
    list_filter = ["status", "lecture_date"]
