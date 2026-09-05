from .utils import log_action

MUTATING_METHODS = {"POST", "PUT", "PATCH", "DELETE"}
# Login/logout/attendance-marking log themselves explicitly with a more
# specific action name; skip them here to avoid duplicate entries.
EXCLUDED_PREFIXES = ("/api/auth/", "/api/attendance", "/admin/")


class AuditLogMiddleware:
    """Coarse-grained fallback: records who changed what via which endpoint
    for any state-changing request not already self-logging (UC-7)."""

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        response = self.get_response(request)

        if (
            request.method in MUTATING_METHODS
            and not request.path.startswith(EXCLUDED_PREFIXES)
            and 200 <= response.status_code < 300
            and getattr(request, "user", None)
            and request.user.is_authenticated
        ):
            entity_type = next((p for p in request.path.strip("/").split("/") if p and p != "api"), "unknown")
            log_action(
                user=request.user,
                request=request,
                action=f"{request.method} {request.path}",
                entity_type=entity_type,
            )

        return response
