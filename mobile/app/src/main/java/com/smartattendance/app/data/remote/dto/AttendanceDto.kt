package com.smartattendance.app.data.remote.dto

import com.google.gson.annotations.SerializedName

object VerificationMethod {
    const val FACE_RECOGNITION = "face_recognition"
    const val MANUAL = "manual"
}

data class MarkAttendanceRequest(
    @SerializedName("session_id") val sessionId: String, // the signed QR token, not a UUID
    @SerializedName("verification_method") val verificationMethod: String = VerificationMethod.FACE_RECOGNITION,
    @SerializedName("scanned_at") val scannedAt: String? = null, // ISO-8601, set when replaying an offline-queued mark
)

data class AttendanceRecordDto(
    val id: String,
    val student: String,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("registration_number") val registrationNumber: String,
    val lecture: String,
    @SerializedName("course_code") val courseCode: String,
    @SerializedName("lecture_date") val lectureDate: String,
    val session: String?,
    @SerializedName("attendance_status") val attendanceStatus: String,
    @SerializedName("verification_method") val verificationMethod: String,
    @SerializedName("marked_at") val markedAt: String,
    @SerializedName("created_at") val createdAt: String,
)

data class MarkAttendanceResponse(
    val status: String,
    val detail: String? = null,
    val attendance: AttendanceRecordDto? = null,
)
