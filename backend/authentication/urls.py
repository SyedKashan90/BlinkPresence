from django.urls import path

from .views import (
    ChangePasswordView,
    LoginView,
    LogoutView,
    MeView,
    PasswordResetConfirmView,
    PasswordResetRequestView,
    RefreshView,
)

urlpatterns = [
    path("auth/login", LoginView.as_view(), name="auth-login"),
    path("auth/refresh", RefreshView.as_view(), name="auth-refresh"),
    path("auth/logout", LogoutView.as_view(), name="auth-logout"),
    path("auth/me", MeView.as_view(), name="auth-me"),
    path("auth/change-password", ChangePasswordView.as_view(), name="auth-change-password"),
    path("auth/password-reset", PasswordResetRequestView.as_view(), name="auth-password-reset"),
    path("auth/password-reset/confirm", PasswordResetConfirmView.as_view(), name="auth-password-reset-confirm"),
]
