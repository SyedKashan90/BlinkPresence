from django.db import transaction
from rest_framework import serializers

from departments.serializers import DepartmentSerializer
from users.models import User
from .models import RegisteredDevice, Student


class RegisteredDeviceSerializer(serializers.ModelSerializer):
    class Meta:
        model = RegisteredDevice
        fields = ["id", "device_id", "device_name", "last_used", "created_at"]
        read_only_fields = ["id", "last_used", "created_at"]


class StudentSerializer(serializers.ModelSerializer):
    department_detail = DepartmentSerializer(source="department", read_only=True)
    email = serializers.EmailField(source="user.email", read_only=True)

    class Meta:
        model = Student
        fields = [
            "id", "user", "email", "registration_number", "full_name", "department", "department_detail",
            "semester", "batch", "phone", "profile_picture", "created_at",
        ]
        read_only_fields = ["id", "user", "email", "created_at"]


class StudentCreateSerializer(serializers.ModelSerializer):
    """Admin-only: provisions the User + Student profile together (UC-7)."""

    email = serializers.EmailField(write_only=True)
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = Student
        fields = [
            "email", "password", "registration_number", "full_name", "department", "semester", "batch", "phone",
        ]

    def validate_email(self, value):
        if User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError("A user with this email address already exists.")
        return value

    @transaction.atomic
    def create(self, validated_data):
        email = validated_data.pop("email")
        password = validated_data.pop("password")
        user = User.objects.create_user(email=email, password=password, role=User.Role.STUDENT)
        return Student.objects.create(user=user, **validated_data)


class StudentSelfUpdateSerializer(serializers.ModelSerializer):
    """FR-2: students may update permitted personal info only — registration
    number, department, and semester stay admin-controlled."""

    class Meta:
        model = Student
        fields = ["phone", "profile_picture"]
