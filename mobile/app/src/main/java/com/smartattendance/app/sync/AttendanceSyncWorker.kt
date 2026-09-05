package com.smartattendance.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smartattendance.app.ServiceLocator

/**
 * Flushes Room-queued offline attendance marks once connectivity returns.
 * The backend re-validates each one's `scanned_at` against
 * ATTENDANCE_OFFLINE_GRACE_MINUTES, so a queued mark can still be rejected
 * (e.g. the grace window lapsed) — that's a normal, expected outcome, not a
 * worker failure.
 */
class AttendanceSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = ServiceLocator.get(applicationContext).attendanceRepository
        return try {
            repo.flushOfflineQueue()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "attendance_offline_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
