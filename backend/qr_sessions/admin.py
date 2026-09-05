from django.contrib import admin

from .models import QRSession


@admin.register(QRSession)
class QRSessionAdmin(admin.ModelAdmin):
    list_display = ["lecture", "is_active", "expires_at", "created_at"]
    list_filter = ["is_active"]
    readonly_fields = ["session_token", "qr_hash"]
