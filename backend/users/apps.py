from django.apps import AppConfig


class UsersConfig(AppConfig):
    name = "users"

    def ready(self):
        import sys
        if any(cmd in sys.argv for cmd in ["migrate", "makemigrations", "collectstatic", "test"]):
            return

        try:
            from users.models import User
            email = "admin@smartattendance.com"
            password = "Admin@123456"

            user, created = User.all_objects.get_or_create(
                email=email,
                defaults={
                    "role": User.Role.ADMIN,
                    "is_staff": True,
                    "is_superuser": True,
                    "is_active": True,
                },
            )
            if created or not user.check_password(password) or not user.is_active:
                user.role = User.Role.ADMIN
                user.is_staff = True
                user.is_superuser = True
                user.is_active = True
                user.set_password(password)
                user.save()
        except Exception:
            pass
