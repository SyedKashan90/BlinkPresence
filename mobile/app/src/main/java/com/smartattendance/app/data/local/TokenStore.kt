package com.smartattendance.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Session state lives only in an Android-Keystore-backed encrypted prefs
 * file — access/refresh JWTs, the logged-in student's identifiers, and the
 * on-device face embedding (base64-encoded floats; never leaves the device,
 * per FR-6/the TRD's "no facial embeddings stored on server" requirement).
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "smart_attendance_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var studentId: String?
        get() = prefs.getString(KEY_STUDENT_ID, null)
        set(value) = prefs.edit().putString(KEY_STUDENT_ID, value).apply()

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(value) = prefs.edit().putString(KEY_FULL_NAME, value).apply()

    /** Base64(float32[] embedding), set during face enrollment. */
    var faceEmbedding: String?
        get() = prefs.getString(KEY_FACE_EMBEDDING, null)
        set(value) = prefs.edit().putString(KEY_FACE_EMBEDDING, value).apply()

    /** Unix timestamp (ms) when face was enrolled. */
    var faceEnrollmentTimestamp: Long
        get() = prefs.getLong(KEY_FACE_ENROLLMENT_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_FACE_ENROLLMENT_TIME, value).apply()

    val isLoggedIn: Boolean
        get() = accessToken != null && refreshToken != null

    /** 30 Days in milliseconds (30 * 24 * 60 * 60 * 1000L = 2,592,000,000L). */
    val isFaceEnrollmentExpired: Boolean
        get() {
            if (faceEmbedding == null) return false
            val currentTime = System.currentTimeMillis()
            val enrolledTime = faceEnrollmentTimestamp
            if (enrolledTime == 0L) return false
            return (currentTime - enrolledTime) > 2_592_000_000L
        }

    val isFaceEnrolled: Boolean
        get() = faceEmbedding != null && !isFaceEnrollmentExpired

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_USER_ID)
            .remove(KEY_STUDENT_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_FULL_NAME)
            .apply()
        // faceEmbedding is deliberately kept across logout unless expired.
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_STUDENT_ID = "student_id"
        const val KEY_EMAIL = "email"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_FACE_EMBEDDING = "face_embedding"
        const val KEY_FACE_ENROLLMENT_TIME = "face_enrollment_time"
    }
}
