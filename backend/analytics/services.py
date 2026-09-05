"""Pre-aggregated analytics (FR-10) — recomputed synchronously on each
attendance write rather than via a scheduled job, since attendance volume
per lecture is small and this keeps the dashboard always up to date."""
from decimal import Decimal, ROUND_HALF_UP

from courses.models import Lecture
from .models import AttendanceStatistics

# A lecture only "counts" toward the denominator once it has actually
# happened, so students aren't marked absent for lectures that haven't
# occurred yet.
COUNTED_LECTURE_STATUSES = [Lecture.Status.ACTIVE, Lecture.Status.COMPLETED]


def refresh_attendance_statistics(*, student, course) -> AttendanceStatistics:
    from attendance.models import Attendance

    total_lectures = Lecture.objects.filter(course=course, status__in=COUNTED_LECTURE_STATUSES).count()
    attended_lectures = Attendance.objects.filter(
        student=student, lecture__course=course, attendance_status=Attendance.Status.PRESENT
    ).count()

    percentage = Decimal(0)
    if total_lectures:
        percentage = (Decimal(attended_lectures) / Decimal(total_lectures) * 100).quantize(
            Decimal("0.01"), rounding=ROUND_HALF_UP
        )

    stats, _created = AttendanceStatistics.objects.update_or_create(
        student=student,
        course=course,
        defaults={
            "total_lectures": total_lectures,
            "attended_lectures": attended_lectures,
            "attendance_percentage": percentage,
        },
    )
    return stats
