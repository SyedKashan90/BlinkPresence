package com.smartattendance.app.data.repository

import com.smartattendance.app.data.local.TokenStore
import com.smartattendance.app.data.remote.ApiResult
import com.smartattendance.app.data.remote.ApiService
import com.smartattendance.app.data.remote.apiCall
import com.smartattendance.app.data.remote.dto.LoginRequest
import com.smartattendance.app.data.remote.dto.LogoutRequest

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    val isLoggedIn: Boolean get() = tokenStore.isLoggedIn
    val isFaceEnrolled: Boolean get() = tokenStore.isFaceEnrolled
    val cachedFullName: String? get() = tokenStore.fullName
    val cachedStudentId: String? get() = tokenStore.studentId

    suspend fun login(email: String, password: String): ApiResult<Unit> {
        val loginResult = apiCall { api.login(LoginRequest(email, password)) }
        if (loginResult !is ApiResult.Success) {
            return when (loginResult) {
                is ApiResult.Error -> ApiResult.Error(loginResult.message, loginResult.code)
                ApiResult.NetworkUnavailable -> ApiResult.NetworkUnavailable
                else -> ApiResult.Error("Login failed")
            }
        }

        val login = loginResult.data
        if (login.role != "student") {
            return ApiResult.Error("This app is for students. Use the web dashboard for admin/teacher accounts.")
        }

        tokenStore.accessToken = login.access
        tokenStore.refreshToken = login.refresh
        tokenStore.userId = login.userId
        tokenStore.email = login.email

        // Resolve the Student.id (distinct from User.id) up front — every
        // history/stats endpoint is keyed by it.
        when (val profile = apiCall { api.myProfile() }) {
            is ApiResult.Success -> {
                tokenStore.studentId = profile.data.id
                tokenStore.fullName = profile.data.fullName
            }
            else -> {
                tokenStore.clearSession()
                return ApiResult.Error("Signed in, but couldn't load your student profile. Please try again.")
            }
        }

        return ApiResult.Success(Unit)
    }

    suspend fun logout() {
        tokenStore.refreshToken?.let { refresh ->
            runCatching { api.logout(LogoutRequest(refresh)) }
        }
        tokenStore.clearSession()
    }
}
