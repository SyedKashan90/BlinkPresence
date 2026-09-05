"""
Local development settings.

Zero-install by default: SQLite, DEBUG on, permissive CORS for the Vite dev
server. If DATABASE_URL is set (e.g. you installed PostgreSQL locally to
mirror production exactly), it takes precedence — see database() below.
"""
from decouple import config

from .base import *  # noqa: F401,F403
from .base import BASE_DIR

DEBUG = True
ALLOWED_HOSTS = ["*"]

_database_url = config("DATABASE_URL", default="")

if _database_url:
    import dj_database_url

    DATABASES = {"default": dj_database_url.parse(_database_url, conn_max_age=600)}
else:
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": BASE_DIR / "db.sqlite3",
        }
    }

# Verbose console logging while developing.
LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "handlers": {"console": {"class": "logging.StreamHandler"}},
    "root": {"handlers": ["console"], "level": "INFO"},
    "loggers": {
        "django.request": {"handlers": ["console"], "level": "ERROR", "propagate": False},
    },
}

EMAIL_BACKEND = "django.core.mail.backends.console.EmailBackend"
