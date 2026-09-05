"""Backend attendance validation chain — FR-8 and Implementation Doc 4.2.3:
1. JWT access token         -> handled by DRF/SimpleJWT auth on the view
2. Validate student + enrolment
3. Validate session + QR expiry (grace window for offline syncs)
4. Duplicate check (Unique(student, lecture), DB-enforced)
5. Store the record + update pre-aggregated statistics
6. Caller emits the real-time WebSocket event
"""
from datetime import timedelta

from django.conf import settings
from django.db import IntegrityError, transaction
from django.utils import timezone

from courses.models import Enrollment
from qr_sessions.services import QRTokenError, decode_qr_token, hash_token
from qr_sessions.models import QRSession
from .models import Attendance


class AttendanceValidationError(Exception):
    """Any failure in the chain — message is safe to surface to the client."""


@transaction.atomic
def mark_attendance(*, student, token: str, verification_method: str, scanned_at=None) -> Attendance:
    now = timezone.now()
    scanned_at = scanned_at or now

    if scanned_at > now + timedelta(minutes=2):
        raise AttendanceValidationError("Scan time cannot be in the future.")

    try:
        payload = decode_qr_token(token)
    except QRTokenError as exc:
        raise AttendanceValidationError(str(exc)) from exc

    try:
        qr_session = (
            QRSession.objects.select_related("lecture", "lecture__course")
            .select_for_update()
            .get(id=payload["session_id"])
        )
    except (QRSession.DoesNotExist, KeyError, ValueError) as exc:
        raise AttendanceValidationError("QR session not found.") from exc

    if qr_session.qr_hash != hash_token(token):
        raise AttendanceValidationError("QR code does not match a known session.")

    grace = timedelta(minutes=settings.ATTENDANCE_OFFLINE_GRACE_MINUTES)
    session_start = qr_session.created_at - timedelta(minutes=1)  # small clock-skew buffer
    session_deadline = qr_session.expires_at + grace
    if not (session_start <= scanned_at <= session_deadline):
        raise AttendanceValidationError("QR code had expired at the time attendance was scanned.")

    lecture = qr_session.lecture

    if not Enrollment.objects.filter(student=student, course_id=lecture.course_id).exists():
        raise AttendanceValidationError("Student is not enrolled in this course.")

    if Attendance.objects.filter(student=student, lecture=lecture).exists():
        raise AttendanceValidationError("Attendance has already been recorded for this lecture.")

    try:
        attendance = Attendance.objects.create(
            student=student,
            lecture=lecture,
            session=qr_session,
            attendance_status=Attendance.Status.PRESENT,
            verification_method=verification_method,
            marked_at=scanned_at,
        )
    except IntegrityError as exc:
        raise AttendanceValidationError("Attendance has already been recorded for this lecture.") from exc

    from analytics.services import refresh_attendance_statistics

    refresh_attendance_statistics(student=student, course=lecture.course)

    from .broadcast import broadcast_attendance_marked

    broadcast_attendance_marked(attendance)

    return attendance
