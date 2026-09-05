from rest_framework.routers import DefaultRouter

from .views import CourseViewSet, EnrollmentViewSet, LectureViewSet

router = DefaultRouter()
router.register("courses", CourseViewSet, basename="course")
router.register("enrollments", EnrollmentViewSet, basename="enrollment")
router.register("lectures", LectureViewSet, basename="lecture")

urlpatterns = router.urls
