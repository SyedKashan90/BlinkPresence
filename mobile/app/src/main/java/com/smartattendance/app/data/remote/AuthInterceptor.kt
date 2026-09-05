package com.smartattendance.app.data.remote

import com.smartattendance.app.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches the current access token to every request except auth endpoints. */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) {
            return chain.proceed(original)
        }

        val token = tokenStore.accessToken ?: return chain.proceed(original)
        val authorized = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }
}
