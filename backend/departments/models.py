from django.db import models

from core.models import UUIDTimeStampedModel


class Department(UUIDTimeStampedModel):
    """BSD Section 4 — `departments` table."""

    department_name = models.CharField(max_length=100)
    department_code = models.CharField(max_length=20, unique=True)

    class Meta:
        db_table = "departments"
        ordering = ["department_name"]

    def __str__(self):
        return f"{self.department_code} — {self.department_name}"
