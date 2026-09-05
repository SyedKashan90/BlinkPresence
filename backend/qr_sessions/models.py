from django.db import models

from core.models import UUIDTimeStampedModel


class QRSession(UUIDTimeStampedModel):
    """BSD Section 10 — `qr_sessions` table: a signed, expiring attendance
    session for one lecture (FR-4)."""

    lecture = models.ForeignKey("courses.Lecture", on_delete=models.CASCADE, related_name="qr_sessions")
    session_token = models.CharField(max_length=500)
    qr_hash = models.CharField(max_length=255, unique=True)
    expires_at = models.DateTimeField()
    is_active = models.BooleanField(default=True)

    class Meta:
        db_table = "qr_sessions"
        indexes = [
            models.Index(fields=["lecture"]),
            models.Index(fields=["expires_at"]),
            models.Index(fields=["is_active"]),
        ]
        ordering = ["-created_at"]

    def __str__(self):
        return f"QR session for {self.lecture} (active={self.is_active})"
