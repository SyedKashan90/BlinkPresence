from rest_framework import viewsets

from authentication.permissions import IsAdmin
from .models import Department
from .serializers import DepartmentSerializer


class DepartmentViewSet(viewsets.ModelViewSet):
    """UC-8-adjacent: admin-only department CRUD, read for any authenticated
    user (teachers/students need it for display purposes)."""

    queryset = Department.objects.all()
    serializer_class = DepartmentSerializer
    filterset_fields = ["department_code"]

    def get_permissions(self):
        if self.action in ("list", "retrieve"):
            return super().get_permissions()
        return [IsAdmin()]
