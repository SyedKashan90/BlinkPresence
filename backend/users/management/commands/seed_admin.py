import os
from django.core.management.base import BaseCommand
from users.models import User

class Command(BaseCommand):
    help = "Seeds an initial admin user in the database if no admin exists."

    def handle(self, *args, **options):
        email = os.environ.get("INITIAL_ADMIN_EMAIL", "admin@smartattendance.com").strip().lower()
        password = os.environ.get("INITIAL_ADMIN_PASSWORD", "Admin@123456")

        user, created = User.all_objects.get_or_create(
            email=email,
            defaults={
                "role": User.Role.ADMIN,
                "is_staff": True,
                "is_superuser": True,
                "is_active": True,
            }
        )

        if created:
            user.set_password(password)
            user.save()
            self.stdout.write(self.style.SUCCESS(f"Successfully created admin user: {email}"))
        else:
            # Ensure password and admin active privileges are set
            user.role = User.Role.ADMIN
            user.is_staff = True
            user.is_superuser = True
            user.is_active = True
            user.set_password(password)
            user.save()
            self.stdout.write(self.style.SUCCESS(f"Updated password and active status for admin user: {email}"))
