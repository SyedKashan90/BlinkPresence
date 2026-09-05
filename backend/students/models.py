from django.conf import settings
from django.db import models

from core.models import SoftDeleteModel, UUIDTimeStampedModel


class Student(UUIDTimeStampedModel, SoftDeleteModel):
    """BSD Section 5 — `students` table.

    Deliberately has no field for a face image or embedding: FR-3/FR-6
    require the reference template to live only in the student's own
    Android Keystore. The server only ever learns *that* a device is bound
    to a student (see RegisteredDevice), never *what* their face looks like.
    """

    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="student_profile")
    registration_number = models.CharField(max_length=50, unique=True)
    full_name = models.CharField(max_length=255)
    department = models.ForeignKey("departments.Department", on_delete=models.PROTECT, related_name="students")
    semester = models.PositiveSmallIntegerField()
    batch = models.CharField(max_length=50, blank=True)
    phone = models.CharField(max_length=20, blank=True)
    profile_picture = models.URLField(blank=True)

    class Meta:
        db_table = "students"
        indexes = [
            models.Index(fields=["department"]),
            models.Index(fields=["semester"]),
        ]
        ordering = ["full_name"]

    def __str__(self):
        return f"{self.registration_number} — {self.full_name}"


class RegisteredDevice(UUIDTimeStampedModel):
    """BSD Section 14 — device binding to deter account/session sharing
    (Section 1.8: 'The registered_devices table mitigates account sharing')."""

    student = models.ForeignKey(Student, on_delete=models.CASCADE, related_name="registered_devices")
    device_id = models.CharField(max_length=255, unique=True)
    device_name = models.CharField(max_length=255, blank=True)
    last_used = models.DateTimeField(null=True, blank=True)

    class Meta:
        db_table = "registered_devices"
        indexes = [models.Index(fields=["student"])]

    def __str__(self):
        return f"{self.device_name or self.device_id} ({self.student.registration_number})"
