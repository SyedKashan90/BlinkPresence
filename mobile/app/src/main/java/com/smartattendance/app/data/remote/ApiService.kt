package com.smartattendance.app.data.remote

import com.smartattendance.app.data.remote.dto.AttendanceRecordDto
import com.smartattendance.app.data.remote.dto.LoginRequest
import com.smartattendance.app.data.remote.dto.LoginResponse
import com.smartattendance.app.data.remote.dto.LogoutRequest
import com.smartattendance.app.data.remote.dto.MarkAttendanceRequest
import com.smartattendance.app.data.remote.dto.MarkAttendanceResponse
import com.smartattendance.app.data.remote.dto.MeResponse
import com.smartattendance.app.data.remote.dto.RefreshRequest
import com.smartattendance.app.data.remote.dto.RefreshResponse
import com.smartattendance.app.data.remote.dto.RegisterDeviceRequest
import com.smartattendance.app.data.remote.dto.RegisteredDeviceDto
import com.smartattendance.app.data.remote.dto.StudentDto
import com.smartattendance.app.data.remote.dto.StudentSelfUpdateRequest
import com.smartattendance.app.data.remote.dto.StudentStatisticsResponse
import com.smartattendance.app.data.remote.dto.ValidateQrRequest
import com.smartattendance.app.data.remote.dto.ValidateQrResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<RefreshResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>

    @GET("api/auth/me")
    suspend fun me(): Response<MeResponse>

    @GET("api/students/me/")
    suspend fun myProfile(): Response<StudentDto>

    @PATCH("api/students/me/update/")
    suspend fun updateMyProfile(@Body body: StudentSelfUpdateRequest): Response<StudentDto>

    @GET("api/students/me/devices/")
    suspend fun myDevices(): Response<List<RegisteredDeviceDto>>

    @POST("api/students/me/devices/")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest): Response<RegisteredDeviceDto>

    @POST("api/session/validate")
    suspend fun validateQr(@Body body: ValidateQrRequest): Response<ValidateQrResponse>

    @POST("api/attendance")
    suspend fun markAttendance(@Body body: MarkAttendanceRequest): Response<MarkAttendanceResponse>

    @GET("api/attendance/student/{studentId}")
    suspend fun attendanceHistory(@Path("studentId") studentId: String): Response<List<AttendanceRecordDto>>

    @GET("api/analytics/student/{studentId}")
    suspend fun studentStatistics(@Path("studentId") studentId: String): Response<StudentStatisticsResponse>
}
