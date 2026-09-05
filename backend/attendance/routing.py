from django.urls import re_path

from .consumers import AttendanceConsumer

websocket_urlpatterns = [
    re_path(r"^ws/attendance/(?P<lecture_id>[0-9a-f-]+)/$", AttendanceConsumer.as_asgi()),
]
