from django.urls import path

from .views import EndSessionView, RefreshSessionView, StartSessionView, ValidateQRView

urlpatterns = [
    path("session/create", StartSessionView.as_view(), name="session-create"),
    path("session/validate", ValidateQRView.as_view(), name="session-validate"),
    path("sessions/<uuid:pk>/refresh", RefreshSessionView.as_view(), name="session-refresh"),
    path("sessions/<uuid:pk>/end", EndSessionView.as_view(), name="session-end"),
]
