package com.smartattendance.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DepartmentDto(
    val id: String,
    @SerializedName("department_name") val departmentName: String,
    @SerializedName("department_code") val departmentCode: String,
)

data class StudentDto(
    val id: String,
    val email: String,
    @SerializedName("registration_number") val registrationNumber: String,
    @SerializedName("full_name") val fullName: String,
    val department: String,
    @SerializedName("department_detail") val departmentDetail: DepartmentDto?,
    val semester: Int,
    val batch: String,
    val phone: String,
    @SerializedName("profile_picture") val profilePicture: String,
)

data class StudentSelfUpdateRequest(
    val phone: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
)

data class RegisteredDeviceDto(
    val id: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("last_used") val lastUsed: String?,
    @SerializedName("created_at") val createdAt: String,
)

data class RegisterDeviceRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
)

data class CourseStatDto(
    val id: String,
    val student: String,
    val course: String,
    @SerializedName("course_code") val courseCode: String,
    @SerializedName("course_name") val courseName: String,
    @SerializedName("total_lectures") val totalLectures: Int,
    @SerializedName("attended_lectures") val attendedLectures: Int,
    @SerializedName("attendance_percentage") val attendancePercentage: Double,
)

data class StudentStatisticsResponse(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("registration_number") val registrationNumber: String,
    @SerializedName("overall_attendance_percentage") val overallAttendancePercentage: Double,
    val courses: List<CourseStatDto>,
)
