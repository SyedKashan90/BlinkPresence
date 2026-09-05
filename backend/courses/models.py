from django.db import models

from core.models import SoftDeleteModel, UUIDTimeStampedModel


class Course(UUIDTimeStampedModel, SoftDeleteModel):
    """BSD Section 7 — `courses` table."""

    course_code = models.CharField(max_length=20, unique=True)
    course_name = models.CharField(max_length=255)
    credit_hours = models.PositiveSmallIntegerField()
    department = models.ForeignKey("departments.Department", on_delete=models.PROTECT, related_name="courses")
    teacher = models.ForeignKey("teachers.Teacher", on_delete=models.SET_NULL, null=True, related_name="courses")
    semester = models.PositiveSmallIntegerField()

    class Meta:
        db_table = "courses"
        indexes = [
            models.Index(fields=["teacher"]),
            models.Index(fields=["semester"]),
        ]
        ordering = ["course_code"]

    def __str__(self):
        return f"{self.course_code} — {self.course_name}"


class Enrollment(UUIDTimeStampedModel):
    """BSD Section 8 — many-to-many between students and courses."""

    student = models.ForeignKey("students.Student", on_delete=models.CASCADE, related_name="enrollments")
    course = models.ForeignKey(Course, on_delete=models.CASCADE, related_name="enrollments")
    enrolled_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "enrollments"
        constraints = [
            models.UniqueConstraint(fields=["student", "course"], name="unique_student_course_enrollment")
        ]

    def __str__(self):
        return f"{self.student.registration_number} → {self.course.course_code}"


class Lecture(UUIDTimeStampedModel):
    """BSD Section 9 — `lectures` table: a scheduled instance of a course."""

    class Status(models.TextChoices):
        SCHEDULED = "scheduled", "Scheduled"
        ACTIVE = "active", "Active"
        COMPLETED = "completed", "Completed"

    course = models.ForeignKey(Course, on_delete=models.CASCADE, related_name="lectures")
    teacher = models.ForeignKey("teachers.Teacher", on_delete=models.SET_NULL, null=True, related_name="lectures")
    lecture_title = models.CharField(max_length=255, blank=True)
    lecture_date = models.DateField()
    start_time = models.TimeField()
    end_time = models.TimeField()
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.SCHEDULED)

    class Meta:
        db_table = "lectures"
        indexes = [
            models.Index(fields=["course"]),
            models.Index(fields=["teacher"]),
            models.Index(fields=["lecture_date"]),
        ]
        ordering = ["-lecture_date", "-start_time"]

    def __str__(self):
        return f"{self.course.course_code} · {self.lecture_date} ({self.status})"
