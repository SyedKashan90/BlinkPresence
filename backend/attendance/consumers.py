from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer

from users.models import User


class AttendanceConsumer(AsyncJsonWebsocketConsumer):
    """UC-5 — Monitor Live Attendance. One group per lecture; every validated
    attendance write (attendance.services.mark_attendance) broadcasts into
    the group for that lecture."""

    async def connect(self):
        self.lecture_id = self.scope["url_route"]["kwargs"]["lecture_id"]
        self.group_name = f"lecture_{self.lecture_id}"

        user = self.scope["user"]
        if not user.is_authenticated or user.role not in (User.Role.ADMIN, User.Role.TEACHER):
            await self.close(code=4403)
            return

        if user.role == User.Role.TEACHER and not await self._teaches_lecture(user):
            await self.close(code=4403)
            return

        await self.channel_layer.group_add(self.group_name, self.channel_name)
        await self.accept()

    async def disconnect(self, close_code):
        if hasattr(self, "group_name"):
            await self.channel_layer.group_discard(self.group_name, self.channel_name)

    @database_sync_to_async
    def _teaches_lecture(self, user):
        from courses.models import Lecture

        return Lecture.objects.filter(id=self.lecture_id, teacher__user=user).exists()

    # Called by channel_layer.group_send(..., {"type": "attendance.marked", ...})
    async def attendance_marked(self, event):
        await self.send_json(event["payload"])
