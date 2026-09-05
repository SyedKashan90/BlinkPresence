from django.contrib import admin
from django.urls import include, path
from drf_spectacular.views import SpectacularAPIView, SpectacularRedocView, SpectacularSwaggerView

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/", include("authentication.urls")),
    path("api/", include("departments.urls")),
    path("api/", include("students.urls")),
    path("api/", include("teachers.urls")),
    path("api/", include("courses.urls")),
    path("api/", include("qr_sessions.urls")),
    path("api/", include("attendance.urls")),
    path("api/", include("reports.urls")),
    path("api/", include("analytics.urls")),
    path("api/", include("notifications.urls")),
    path("api/", include("audit_logs.urls")),
    # OpenAPI / Swagger (Implementation Doc 4.3.1: "The API is documented with Swagger/OpenAPI")
    path("api/schema/", SpectacularAPIView.as_view(), name="schema"),
    path("api/docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),
    path("api/redoc/", SpectacularRedocView.as_view(url_name="schema"), name="redoc"),
]
