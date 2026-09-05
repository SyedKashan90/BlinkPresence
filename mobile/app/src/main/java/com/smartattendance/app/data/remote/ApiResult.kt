package com.smartattendance.app.data.remote

import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data object NetworkUnavailable : ApiResult<Nothing>()
}

private val gson = Gson()

/** Best-effort extraction of the backend's `{"detail": "..."}` / `{"status":"failed","detail":"..."}` error shape. */
fun Response<*>.errorDetail(): String {
    val raw = errorBody()?.string()
    if (raw.isNullOrBlank()) return "Request failed (${code()})"
    return runCatching {
        val map = gson.fromJson(raw, Map::class.java)
        (map["detail"] ?: map["error"])?.toString()
    }.getOrNull() ?: raw
}

suspend fun <T> apiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body)
        } else {
            ApiResult.Error(response.errorDetail(), response.code())
        }
    } catch (e: IOException) {
        ApiResult.NetworkUnavailable
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unexpected error")
    }
}
