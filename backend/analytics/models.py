from django.db import models

from core.models import UUIDTimeStampedModel


class AttendanceStatistics(UUIDTimeStampedModel):
    """BSD Section 12 — `attendance_statistics` table: pre-calculated
    per-student-per-course analytics (FR-10), refreshed synchronously each
    time an attendance record is written (see analytics.services)."""

    student = models.ForeignKey("students.Student", on_delete=models.CASCADE, related_name="statistics")
    course = models.ForeignKey("courses.Course", on_delete=models.CASCADE, related_name="statistics")
    total_lectures = models.PositiveIntegerField(default=0)
    attended_lectures = models.PositiveIntegerField(default=0)
    attendance_percentage = models.DecimalField(max_digits=5, decimal_places=2, default=0)

    class Meta:
        db_table = "attendance_statistics"
        constraints = [
            models.UniqueConstraint(fields=["student", "course"], name="unique_student_course_statistics")
        ]

    def __str__(self):
        return f"{self.student.registration_number} · {self.course.course_code} · {self.attendance_percentage}%"
