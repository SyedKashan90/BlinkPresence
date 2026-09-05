from rest_framework import serializers

from .models import QRSession


class QRSessionSerializer(serializers.ModelSerializer):
    course_id = serializers.UUIDField(source="lecture.course_id", read_only=True)
    course_code = serializers.CharField(source="lecture.course.course_code", read_only=True)
    expires_in_seconds = serializers.SerializerMethodField()
    qr_image_base64 = serializers.SerializerMethodField()

    class Meta:
        model = QRSession
        fields = [
            "id", "lecture", "course_id", "course_code", "session_token", "expires_at",
            "expires_in_seconds", "is_active", "qr_image_base64", "created_at",
        ]
        read_only_fields = fields

    def get_expires_in_seconds(self, obj):
        from django.utils import timezone

        remaining = (obj.expires_at - timezone.now()).total_seconds()
        return max(0, int(remaining))

    def get_qr_image_base64(self, obj):
        return self.context.get("qr_image_base64")


class StartSessionSerializer(serializers.Serializer):
    course = serializers.UUIDField()
    lecture_title = serializers.CharField(required=False, allow_blank=True, default="")


class ValidateQRSerializer(serializers.Serializer):
    token = serializers.CharField()
