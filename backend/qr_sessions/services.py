"""Dynamic QR generation/validation — FR-4 and FR-5, and the algorithms
described in the FYP report Section 4.2.1 (Dynamic QR Generation) and
4.2.3 (Backend Attendance Validation, steps 1–4).

The QR payload is a signed JWT (not plain JSON) so a client can fast-reject
locally by reading the `exp` claim, but only the server — which holds
QR_SIGNING_KEY — can prove the token wasn't forged.
"""
import hashlib
from datetime import timedelta

import jwt
from django.conf import settings
from django.utils import timezone

from courses.models import Lecture
from .models import QRSession

ALGORITHM = "HS256"


class QRTokenError(Exception):
    """Raised for any invalid/expired/tampered QR token — callers turn this
    into the appropriate 4xx response."""


def hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def issue_qr_session(lecture: Lecture) -> QRSession:
    """Create a new signed QR session for `lecture`, invalidating any
    still-active session for the same lecture (FR-4: 'cannot be reused';
    'previous token is invalidated' on refresh)."""
    now = timezone.now()
    expires_at = now + timedelta(minutes=settings.QR_SESSION_EXPIRY_MINUTES)

    QRSession.objects.filter(lecture=lecture, is_active=True).update(is_active=False)

    # session_id is assigned after the row exists so it can be embedded in
    # its own signed payload; write the row first with a placeholder token.
    qr_session = QRSession.objects.create(lecture=lecture, session_token="", qr_hash="", expires_at=expires_at)

    payload = {
        "session_id": str(qr_session.id),
        "lecture_id": str(lecture.id),
        "course_id": str(lecture.course_id),
        "teacher_id": str(lecture.teacher_id) if lecture.teacher_id else None,
        "expires_at": expires_at.isoformat(),
        "iat": now,
        "exp": expires_at,
    }
    token = jwt.encode(payload, settings.QR_SIGNING_KEY, algorithm=ALGORITHM)

    qr_session.session_token = token
    qr_session.qr_hash = hash_token(token)
    qr_session.save(update_fields=["session_token", "qr_hash"])
    return qr_session


def decode_qr_token(token: str) -> dict:
    """Verify signature only — NOT expiry. Expiry is checked explicitly by
    callers against either 'now' (live scans) or the client-supplied scan
    timestamp (offline syncs, within a grace window). Raises QRTokenError on
    a bad signature/malformed token; callers should not distinguish *why*
    to an unauthenticated scanner beyond 'invalid or expired'."""
    try:
        return jwt.decode(token, settings.QR_SIGNING_KEY, algorithms=[ALGORITHM], options={"verify_exp": False})
    except jwt.InvalidTokenError as exc:
        raise QRTokenError("QR code is invalid.") from exc


def get_active_session_for_token(token: str) -> QRSession:
    """Full server-side validation chain (FR-5 / Implementation 4.2.3
    steps 1–4): signature+expiry, then the session must still be the
    lecture's active one and not separately deactivated."""
    payload = decode_qr_token(token)

    try:
        qr_session = QRSession.objects.select_related("lecture", "lecture__course").get(id=payload["session_id"])
    except (QRSession.DoesNotExist, KeyError, ValueError) as exc:
        raise QRTokenError("QR session not found.") from exc

    if not qr_session.is_active:
        raise QRTokenError("This QR code has been superseded by a newer one.")
    if qr_session.expires_at <= timezone.now():
        raise QRTokenError("QR code has expired.")
    if qr_session.qr_hash != hash_token(token):
        # Defence in depth: the JWT decoded fine but doesn't match the row
        # it claims to be (e.g. a stale/rotated signing key edge case).
        raise QRTokenError("QR code does not match an active session.")

    return qr_session


def end_session(qr_session: QRSession) -> None:
    qr_session.is_active = False
    qr_session.save(update_fields=["is_active"])
    lecture = qr_session.lecture
    lecture.status = Lecture.Status.COMPLETED
    lecture.save(update_fields=["status"])
