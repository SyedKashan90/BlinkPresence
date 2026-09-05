import base64
from datetime import timedelta
from io import BytesIO

import qrcode
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.exceptions import PermissionDenied
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from rest_framework.views import APIView

from audit_logs.utils import log_action
from authentication.permissions import IsAdminOrTeacher, IsStudent
from courses.models import Course, Lecture
from teachers.models import Teacher
from users.models import User
from .models import QRSession
from .serializers import QRSessionSerializer, StartSessionSerializer, ValidateQRSerializer
from .services import QRTokenError, end_session, get_active_session_for_token, issue_qr_session


def _qr_image_base64(token: str) -> str:
    img = qrcode.make(token)
    buffer = BytesIO()
    img.save(buffer, format="PNG")
    return base64.b64encode(buffer.getvalue()).decode()


def _teacher_owns_course(user, course: Course) -> bool:
    if user.role == User.Role.ADMIN:
        return True
    return course.teacher_id is not None and course.teacher.user_id == user.id


class StartSessionView(APIView):
    """POST /api/session/create — UC-4 / FR-4. Finds-or-creates today's
    lecture for the course and issues a fresh signed, expiring QR."""

    permission_classes = [IsAdminOrTeacher]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "qr_session"

    def post(self, request):
        serializer = StartSessionSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        course = get_object_or_404(Course, id=serializer.validated_data["course"])

        if not _teacher_owns_course(request.user, course):
            raise PermissionDenied("You are not assigned to teach this course.")

        teacher = course.teacher
        if request.user.role == User.Role.TEACHER and teacher is None:
            teacher = Teacher.objects.get(user=request.user)

        now = timezone.now()
        lecture = (
            Lecture.objects.filter(
                course=course, lecture_date=now.date(), status__in=[Lecture.Status.SCHEDULED, Lecture.Status.ACTIVE]
            )
            .order_by("-start_time")
            .first()
        )
        if lecture is None:
            lecture = Lecture.objects.create(
                course=course,
                teacher=teacher,
                lecture_title=serializer.validated_data.get("lecture_title", ""),
                lecture_date=now.date(),
                start_time=now.time(),
                end_time=(now + timedelta(hours=1)).time(),
                status=Lecture.Status.ACTIVE,
            )
        elif lecture.status != Lecture.Status.ACTIVE:
            lecture.status = Lecture.Status.ACTIVE
            lecture.save(update_fields=["status"])

        qr_session = issue_qr_session(lecture)
        log_action(user=request.user, request=request, action="start_qr_session", entity_type="QRSession", entity_id=qr_session.id)

        data = QRSessionSerializer(qr_session, context={"qr_image_base64": _qr_image_base64(qr_session.session_token)}).data
        return Response(data, status=status.HTTP_201_CREATED)


class RefreshSessionView(APIView):
    """POST /api/sessions/{id}/refresh — FR-4: 'auto-refreshes before
    expiry; the previous token is invalidated.'"""

    permission_classes = [IsAdminOrTeacher]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "qr_session"

    def post(self, request, pk):
        qr_session = get_object_or_404(QRSession.objects.select_related("lecture__course"), pk=pk)
        if not _teacher_owns_course(request.user, qr_session.lecture.course):
            raise PermissionDenied("You are not assigned to teach this course.")

        new_session = issue_qr_session(qr_session.lecture)
        data = QRSessionSerializer(
            new_session, context={"qr_image_base64": _qr_image_base64(new_session.session_token)}
        ).data
        return Response(data)


class EndSessionView(APIView):
    """POST /api/sessions/{id}/end — UC-5: 'Teacher ends the session when
    finished.'"""

    permission_classes = [IsAdminOrTeacher]

    def post(self, request, pk):
        qr_session = get_object_or_404(QRSession.objects.select_related("lecture__course"), pk=pk)
        if not _teacher_owns_course(request.user, qr_session.lecture.course):
            raise PermissionDenied("You are not assigned to teach this course.")

        end_session(qr_session)
        log_action(user=request.user, request=request, action="end_qr_session", entity_type="QRSession", entity_id=qr_session.id)
        return Response(QRSessionSerializer(qr_session).data)


class ValidateQRView(APIView):
    """POST /api/session/validate — FR-5 authoritative check, used by the
    mobile app as a pre-flight before running face verification + liveness."""

    permission_classes = [IsStudent]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "qr_session"

    def post(self, request):
        serializer = ValidateQRSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            qr_session = get_active_session_for_token(serializer.validated_data["token"])
        except QRTokenError as exc:
            return Response({"valid": False, "detail": str(exc)}, status=status.HTTP_200_OK)

        return Response(
            {
                "valid": True,
                "session_id": qr_session.id,
                "lecture_id": qr_session.lecture_id,
                "course_id": qr_session.lecture.course_id,
                "course_code": qr_session.lecture.course.course_code,
                "expires_at": qr_session.expires_at,
            }
        )
