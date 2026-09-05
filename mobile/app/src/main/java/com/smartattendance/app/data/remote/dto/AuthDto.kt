package com.smartattendance.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val access: String,
    val refresh: String,
    val role: String,
    @SerializedName("user_id") val userId: String,
    val email: String,
)

data class RefreshRequest(
    val refresh: String,
)

data class RefreshResponse(
    val access: String,
    val refresh: String? = null,
)

data class LogoutRequest(
    val refresh: String,
)

data class MeResponse(
    val id: String,
    val email: String,
    val role: String,
)
