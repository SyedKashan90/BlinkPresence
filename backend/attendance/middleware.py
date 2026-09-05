"""JWT auth for WebSocket connections (UC-5 live attendance monitoring).

Browsers can't attach an Authorization header to a WebSocket handshake, so
the access token is passed as a query parameter instead:
    wss://.../ws/attendance/<lecture_id>/?token=<access_token>
"""
from urllib.parse import parse_qs

from channels.db import database_sync_to_async
from channels.middleware import BaseMiddleware
from django.contrib.auth.models import AnonymousUser
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
from rest_framework_simplejwt.tokens import AccessToken


@database_sync_to_async
def _get_user(user_id):
    from users.models import User

    try:
        return User.objects.get(id=user_id)
    except User.DoesNotExist:
        return AnonymousUser()


class JWTAuthMiddleware(BaseMiddleware):
    async def __call__(self, scope, receive, send):
        query_string = parse_qs(scope.get("query_string", b"").decode())
        token = query_string.get("token", [None])[0]

        scope["user"] = AnonymousUser()
        if token:
            try:
                access_token = AccessToken(token)
                scope["user"] = await _get_user(access_token["user_id"])
            except (InvalidToken, TokenError, KeyError):
                scope["user"] = AnonymousUser()

        return await super().__call__(scope, receive, send)


def JWTAuthMiddlewareStack(inner):
    return JWTAuthMiddleware(inner)
