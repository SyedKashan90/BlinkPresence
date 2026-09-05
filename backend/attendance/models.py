from django.db import models

from core.models import UUIDTimeStampedModel


class Attendance(UUIDTimeStampedModel):
    """BSD Section 11 — `attendance` table: the validated attendance ledger.

    Unique(student, lecture) is the server-side backstop for FR-8's
    duplicate-prevention requirement — it is enforced at the database level,
    not just in application code, so a race between two concurrent requests
    can't double-mark the same student.
    """

    class Status(models.TextChoices):
        PRESENT = "present", "Present"
        ABSENT = "absent", "Absent"
        LATE = "late", "Late"

    class VerificationMethod(models.TextChoices):
        FACE_RECOGNITION = "face_recognition", "Face Recognition"
        MANUAL = "manual", "Manual"

    student = models.ForeignKey("students.Student", on_delete=models.CASCADE, related_name="attendance_records")
    lecture = models.ForeignKey("courses.Lecture", on_delete=models.CASCADE, related_name="attendance_records")
    session = models.ForeignKey(
        "qr_sessions.QRSession", on_delete=models.SET_NULL, null=True, blank=True, related_name="attendance_records"
    )
    attendance_status = models.CharField(max_length=20, choices=Status.choices, default=Status.PRESENT)
    verification_method = models.CharField(max_length=20, choices=VerificationMethod.choices)
    marked_at = models.DateTimeField()

    class Meta:
        db_table = "attendance"
        constraints = [
            models.UniqueConstraint(fields=["student", "lecture"], name="unique_student_lecture_attendance")
        ]
        indexes = [
            models.Index(fields=["student"]),
            models.Index(fields=["lecture"]),
            models.Index(fields=["marked_at"]),
        ]
        ordering = ["-marked_at"]

    def __str__(self):
        return f"{self.student.registration_number} · {self.lecture} · {self.attendance_status}"
