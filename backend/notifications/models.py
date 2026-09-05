from django.conf import settings
from django.db import models

from core.models import UUIDTimeStampedModel


class Notification(UUIDTimeStampedModel):
    """BSD Section 13 — `notifications` table (attendance alerts, defaulter
    warnings, etc. per FR-1/System Components: 'Receive attendance alerts')."""

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="notifications")
    title = models.CharField(max_length=255)
    message = models.TextField()
    is_read = models.BooleanField(default=False)

    class Meta:
        db_table = "notifications"
        indexes = [
            models.Index(fields=["user"]),
            models.Index(fields=["is_read"]),
        ]
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.title} → {self.user.email}"
