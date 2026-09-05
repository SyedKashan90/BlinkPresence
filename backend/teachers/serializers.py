from django.db import transaction
from rest_framework import serializers

from departments.serializers import DepartmentSerializer
from users.models import User
from .models import Teacher


class TeacherSerializer(serializers.ModelSerializer):
    department_detail = DepartmentSerializer(source="department", read_only=True)
    email = serializers.EmailField(source="user.email", read_only=True)

    class Meta:
        model = Teacher
        fields = [
            "id", "user", "email", "employee_id", "full_name", "department", "department_detail",
            "designation", "phone", "created_at",
        ]
        read_only_fields = ["id", "user", "email", "created_at"]


class TeacherCreateSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(write_only=True)
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = Teacher
        fields = ["email", "password", "employee_id", "full_name", "department", "designation", "phone"]

    def validate_email(self, value):
        if User.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError("A user with this email address already exists.")
        return value

    @transaction.atomic
    def create(self, validated_data):
        email = validated_data.pop("email")
        password = validated_data.pop("password")
        user = User.objects.create_user(email=email, password=password, role=User.Role.TEACHER)
        return Teacher.objects.create(user=user, **validated_data)
