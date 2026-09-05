package com.smartattendance.app.data.repository

import com.smartattendance.app.data.local.TokenStore
import com.smartattendance.app.data.remote.ApiResult
import com.smartattendance.app.data.remote.ApiService
import com.smartattendance.app.data.remote.apiCall
import com.smartattendance.app.data.remote.dto.RegisterDeviceRequest
import com.smartattendance.app.data.remote.dto.RegisteredDeviceDto
import com.smartattendance.app.data.remote.dto.StudentDto
import com.smartattendance.app.data.remote.dto.StudentSelfUpdateRequest
import com.smartattendance.app.data.remote.dto.StudentStatisticsResponse

class StudentRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    suspend fun profile(): ApiResult<StudentDto> = apiCall { api.myProfile() }

    suspend fun updateProfile(phone: String?, profilePicture: String?): ApiResult<StudentDto> =
        apiCall { api.updateMyProfile(StudentSelfUpdateRequest(phone, profilePicture)) }

    suspend fun statistics(): ApiResult<StudentStatisticsResponse> {
        val studentId = tokenStore.studentId ?: return ApiResult.Error("Not signed in")
        return apiCall { api.studentStatistics(studentId) }
    }

    suspend fun registeredDevices(): ApiResult<List<RegisteredDeviceDto>> = apiCall { api.myDevices() }

    suspend fun registerDevice(deviceId: String, deviceName: String): ApiResult<RegisteredDeviceDto> =
        apiCall { api.registerDevice(RegisterDeviceRequest(deviceId, deviceName)) }
}
