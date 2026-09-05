from django.conf import settings
from django.db import models

from core.models import SoftDeleteModel, UUIDTimeStampedModel


class Teacher(UUIDTimeStampedModel, SoftDeleteModel):
    """BSD Section 6 — `teachers` table."""

    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="teacher_profile")
    employee_id = models.CharField(max_length=50, unique=True)
    full_name = models.CharField(max_length=255)
    department = models.ForeignKey("departments.Department", on_delete=models.PROTECT, related_name="teachers")
    designation = models.CharField(max_length=100, blank=True)
    phone = models.CharField(max_length=20, blank=True)

    class Meta:
        db_table = "teachers"
        indexes = [models.Index(fields=["department"])]
        ordering = ["full_name"]

    def __str__(self):
        return f"{self.employee_id} — {self.full_name}"
