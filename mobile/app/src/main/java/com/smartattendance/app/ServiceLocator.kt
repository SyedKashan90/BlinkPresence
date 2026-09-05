package com.smartattendance.app

import android.content.Context
import com.smartattendance.app.data.local.AppDatabase
import com.smartattendance.app.data.local.TokenStore
import com.smartattendance.app.data.remote.ApiService
import com.smartattendance.app.data.remote.NetworkModule
import com.smartattendance.app.data.repository.AttendanceRepository
import com.smartattendance.app.data.repository.AuthRepository
import com.smartattendance.app.data.repository.StudentRepository
import com.smartattendance.app.face.FaceEmbedder

/**
 * Hand-rolled DI container (no Hilt/Dagger, to keep the build lean for an
 * FYP-scoped app). Everything here is a process-wide singleton created
 * lazily on first access.
 */
class ServiceLocator(context: Context) {
    private val appContext = context.applicationContext

    val tokenStore: TokenStore by lazy { TokenStore(appContext) }
    val apiService: ApiService by lazy { NetworkModule.buildApiService(tokenStore) }
    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val faceEmbedder: FaceEmbedder by lazy { FaceEmbedder(appContext) }

    val authRepository: AuthRepository by lazy { AuthRepository(apiService, tokenStore) }
    val studentRepository: StudentRepository by lazy { StudentRepository(apiService, tokenStore) }
    val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepository(apiService, tokenStore, database.offlineAttendanceDao(), database.cachedAttendanceDao())
    }

    companion object {
        @Volatile private var instance: ServiceLocator? = null

        fun get(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context).also { instance = it }
            }
    }
}
