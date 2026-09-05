from .models import AuditLog


def _client_ip(request):
    forwarded = request.META.get("HTTP_X_FORWARDED_FOR")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.META.get("REMOTE_ADDR")


def log_action(*, user, request, action, entity_type, entity_id=None):
    """Best-effort audit trail write — never raises, so a logging failure
    can't take down the request it's trying to record."""
    try:
        AuditLog.objects.create(
            user=user if user and getattr(user, "is_authenticated", False) else None,
            action=action,
            entity_type=entity_type,
            entity_id=entity_id,
            ip_address=_client_ip(request) if request else None,
        )
    except Exception:  # noqa: BLE001 — audit logging must never break the request
        pass
