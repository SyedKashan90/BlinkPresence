package com.smartattendance.app.data.repository

import com.smartattendance.app.data.local.CachedAttendanceDao
import com.smartattendance.app.data.local.CachedAttendanceEntity
import com.smartattendance.app.data.local.OfflineAttendanceDao
import com.smartattendance.app.data.local.OfflineAttendanceEntity
import com.smartattendance.app.data.local.TokenStore
import com.smartattendance.app.data.remote.ApiResult
import com.smartattendance.app.data.remote.ApiService
import com.smartattendance.app.data.remote.apiCall
import com.smartattendance.app.data.remote.dto.AttendanceRecordDto
import com.smartattendance.app.data.remote.dto.MarkAttendanceRequest
import com.smartattendance.app.data.remote.dto.MarkAttendanceResponse
import com.smartattendance.app.data.remote.dto.ValidateQrResponse
import com.smartattendance.app.data.remote.dto.VerificationMethod
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

sealed class MarkOutcome {
    data class Success(val response: MarkAttendanceResponse) : MarkOutcome()
    data class Rejected(val reason: String) : MarkOutcome()
    data object QueuedOffline : MarkOutcome()
}

class AttendanceRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val offlineDao: OfflineAttendanceDao,
    private val cachedDao: CachedAttendanceDao,
) {
    suspend fun validateQr(token: String): ApiResult<ValidateQrResponse> =
        apiCall { api.validateQr(com.smartattendance.app.data.remote.dto.ValidateQrRequest(token)) }

    /**
     * Marks attendance right away when online; if the network call itself
     * fails (not a validation rejection from the backend), the mark is
     * queued in Room and flushed later by [AttendanceSyncWorker] using this
     * same `scannedAt` timestamp — matching the offline-grace-window design
     * in attendance/services.py on the backend.
     */
    suspend fun markAttendance(
        qrToken: String,
        courseCodeHint: String,
        scannedAt: Instant = Instant.now(),
    ): MarkOutcome {
        val scannedAtIso = ISO_FORMAT.format(scannedAt.atOffset(ZoneOffset.UTC))
        val request = MarkAttendanceRequest(
            sessionId = qrToken,
            verificationMethod = VerificationMethod.FACE_RECOGNITION,
            scannedAt = scannedAtIso,
        )

        return when (val result = apiCall { api.markAttendance(request) }) {
            is ApiResult.Success -> MarkOutcome.Success(result.data)
            is ApiResult.Error -> MarkOutcome.Rejected(result.message)
            ApiResult.NetworkUnavailable -> {
                offlineDao.enqueue(
                    OfflineAttendanceEntity(
                        qrToken = qrToken,
                        verificationMethod = VerificationMethod.FACE_RECOGNITION,
                        scannedAtIso = scannedAtIso,
                        courseCodeHint = courseCodeHint,
                        createdAtIso = scannedAtIso,
                    )
                )
                MarkOutcome.QueuedOffline
            }
        }
    }

    /** Called by the background sync worker; returns how many entries were successfully flushed. */
    suspend fun flushOfflineQueue(): Int {
        var synced = 0
        for (entry in offlineDao.all()) {
            val request = MarkAttendanceRequest(
                sessionId = entry.qrToken,
                verificationMethod = entry.verificationMethod,
                scannedAt = entry.scannedAtIso,
            )
            when (val result = apiCall { api.markAttendance(request) }) {
                is ApiResult.Success -> {
                    offlineDao.delete(entry)
                    synced++
                }
                is ApiResult.Error -> {
                    // A definitive rejection (expired, duplicate, etc.) — retrying won't help.
                    offlineDao.delete(entry)
                }
                ApiResult.NetworkUnavailable -> {
                    offlineDao.update(entry.copy(attempts = entry.attempts + 1, lastError = "offline"))
                }
            }
        }
        return synced
    }

    fun observePendingSyncCount(): Flow<Int> = offlineDao.observePendingCount()

    fun observeCachedHistory(): Flow<List<CachedAttendanceEntity>> = cachedDao.observeAll()

    suspend fun refreshHistory(): ApiResult<List<AttendanceRecordDto>> {
        val studentId = tokenStore.studentId ?: return ApiResult.Error("Not signed in")
        val result = apiCall { api.attendanceHistory(studentId) }
        if (result is ApiResult.Success) {
            cachedDao.upsertAll(
                result.data.map {
                    CachedAttendanceEntity(
                        id = it.id,
                        courseCode = it.courseCode,
                        lectureDate = it.lectureDate,
                        attendanceStatus = it.attendanceStatus,
                        verificationMethod = it.verificationMethod,
                        markedAt = it.markedAt,
                    )
                }
            )
        }
        return result
    }

    private companion object {
        val ISO_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    }
}
