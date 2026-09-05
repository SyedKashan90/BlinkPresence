package com.smartattendance.app.data.remote

import com.google.gson.Gson
import com.smartattendance.app.BuildConfig
import com.smartattendance.app.data.local.TokenStore
import com.smartattendance.app.data.remote.dto.RefreshRequest
import com.smartattendance.app.data.remote.dto.RefreshResponse
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * Mirrors the web dashboard's axios "refresh-on-401" interceptor: on a 401,
 * synchronously swap the refresh token for a new access token and retry
 * once. `synchronized` collapses concurrent 401s onto a single refresh call,
 * same as the web client's `refreshPromise` dedup.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val plainClient: OkHttpClient,
) : Authenticator {

    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // already retried once, give up

        val refreshToken = tokenStore.refreshToken ?: return null
        val failedAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(this) {
            // Another thread may have already refreshed while we waited for the lock.
            val currentAccess = tokenStore.accessToken
            if (currentAccess != null && currentAccess != failedAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .build()
            }

            val newTokens = runCatching { refreshSync(refreshToken) }.getOrNull()
            if (newTokens == null) {
                tokenStore.clearSession()
                return null
            }

            tokenStore.accessToken = newTokens.access
            newTokens.refresh?.let { tokenStore.refreshToken = it }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.access}")
                .build()
        }
    }

    private fun refreshSync(refreshToken: String): RefreshResponse? {
        val json = "application/json; charset=utf-8".toMediaType()
        val body = gson.toJson(RefreshRequest(refreshToken)).toRequestBody(json)
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "api/auth/refresh")
            .post(body)
            .build()

        plainClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val bodyString = resp.body?.string() ?: return null
            return gson.fromJson(bodyString, RefreshResponse::class.java)
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
