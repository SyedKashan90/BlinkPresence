import uuid

from django.conf import settings
from django.db import models


class AuditLog(models.Model):
    """BSD Section 15 — `audit_logs` table. Tracks sensitive operations
    (UC-7: 'Action is recorded in audit_logs')."""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True, blank=True, related_name="audit_logs"
    )
    action = models.CharField(max_length=255)
    entity_type = models.CharField(max_length=100)
    entity_id = models.UUIDField(null=True, blank=True)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "audit_logs"
        ordering = ["-created_at"]
        indexes = [
            models.Index(fields=["user"]),
            models.Index(fields=["created_at"]),
        ]

    def __str__(self):
        actor = self.user.email if self.user else "anonymous"
        return f"{actor} · {self.action} · {self.entity_type} · {self.created_at:%Y-%m-%d %H:%M}"
