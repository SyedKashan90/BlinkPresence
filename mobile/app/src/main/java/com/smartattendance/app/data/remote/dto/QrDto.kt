package com.smartattendance.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ValidateQrRequest(
    val token: String,
)

data class ValidateQrResponse(
    val valid: Boolean,
    val detail: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("lecture_id") val lectureId: String? = null,
    @SerializedName("course_id") val courseId: String? = null,
    @SerializedName("course_code") val courseCode: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
)
