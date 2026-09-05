"""Sync-context helper to push a live update after an attendance record is
written (Implementation 4.2.2 step 7: 'broadcast the update to the teacher
dashboard over WebSocket'). Called from the (synchronous) DRF view/service."""
from asgiref.sync import async_to_sync
from channels.layers import get_channel_layer


def broadcast_attendance_marked(attendance):
    channel_layer = get_channel_layer()
    if channel_layer is None:
        return

    student = attendance.student
    payload = {
        "type": "attendance_marked",
        "attendance_id": str(attendance.id),
        "student_id": str(student.id),
        "student_name": student.full_name,
        "registration_number": student.registration_number,
        "attendance_status": attendance.attendance_status,
        "verification_method": attendance.verification_method,
        "marked_at": attendance.marked_at.isoformat(),
    }

    async_to_sync(channel_layer.group_send)(
        f"lecture_{attendance.lecture_id}",
        {"type": "attendance.marked", "payload": payload},
    )
