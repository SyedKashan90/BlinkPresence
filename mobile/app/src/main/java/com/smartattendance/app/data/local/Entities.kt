package com.smartattendance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Attendance marks captured while offline (or while the mark request
 * failed transiently). `scannedAt` is the client-side moment verification
 * succeeded — sent to the backend so it can validate against
 * ATTENDANCE_OFFLINE_GRACE_MINUTES instead of "now" once connectivity
 * returns (see attendance/services.py mark_attendance on the backend).
 */
@Entity(tableName = "offline_attendance_queue")
data class OfflineAttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val qrToken: String,
    val verificationMethod: String,
    val scannedAtIso: String,
    val courseCodeHint: String,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAtIso: String,
)

/** Local cache of the student's own attendance ledger for offline viewing. */
@Entity(tableName = "cached_attendance")
data class CachedAttendanceEntity(
    @PrimaryKey val id: String,
    val courseCode: String,
    val lectureDate: String,
    val attendanceStatus: String,
    val verificationMethod: String,
    val markedAt: String,
)
