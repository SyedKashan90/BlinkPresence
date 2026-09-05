package com.smartattendance.app.face

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Cosine-similarity matching against the on-device-enrolled embedding.
 * [DEFAULT_THRESHOLD] mirrors the backend's FACE_MATCH_COSINE_THRESHOLD
 * (config/settings/base.py) purely so the two systems agree conceptually —
 * the backend never receives an embedding to check itself (FR-06).
 */
object FaceMatcher {
    const val DEFAULT_THRESHOLD = 0.75f

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0.0
        for (v in vector) sumSq += v.toDouble() * v.toDouble()
        val norm = sqrt(sumSq).toFloat().coerceAtLeast(1e-8f)
        return FloatArray(vector.size) { vector[it] / norm }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return -1f
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot // both vectors are already L2-normalized, so dot product == cosine similarity
    }

    fun isMatch(candidate: FloatArray, enrolled: FloatArray, threshold: Float = DEFAULT_THRESHOLD): Boolean =
        cosineSimilarity(candidate, enrolled) >= threshold

    fun encode(embedding: FloatArray): String {
        val buffer = ByteBuffer.allocate(4 * embedding.size).order(ByteOrder.nativeOrder())
        embedding.forEach { buffer.putFloat(it) }
        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    fun decode(base64: String): FloatArray {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) floats[i] = buffer.float
        return floats
    }
}
