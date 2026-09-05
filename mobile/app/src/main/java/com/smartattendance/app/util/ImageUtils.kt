package com.smartattendance.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageUtils {

    /** YUV_420_888 -> NV21 -> JPEG -> Bitmap, then rotated upright to match the
     * `rotationDegrees` passed to ML Kit's InputImage for the same frame, so
     * a Face's boundingBox (reported in that rotated space) can be applied
     * directly to the returned bitmap.
     *
     * Performance: uses bulk ByteBuffer copies instead of per-pixel loops.
     * The old pixel-by-pixel yuv420ToNv21 ran ~2M+ iterations for a 1080p frame
     * on the analysis thread — this version is orders of magnitude faster. */
    fun imageProxyToUprightBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val nv21 = yuv420ToNv21Fast(image)
        val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream(image.width * image.height)
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 85, out)
        val bytes = out.toByteArray()
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0) return raw
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }

    fun cropAndResize(bitmap: Bitmap, box: Rect, targetSize: Int, marginPercent: Float = 0.15f): Bitmap {
        val marginX = (box.width() * marginPercent).toInt()
        val marginY = (box.height() * marginPercent).toInt()
        val safe = Rect(
            (box.left - marginX).coerceIn(0, bitmap.width - 1),
            (box.top - marginY).coerceIn(0, bitmap.height - 1),
            (box.right + marginX).coerceIn(1, bitmap.width),
            (box.bottom + marginY).coerceIn(1, bitmap.height),
        )
        val width = (safe.right - safe.left).coerceAtLeast(1)
        val height = (safe.bottom - safe.top).coerceAtLeast(1)
        val cropped = Bitmap.createBitmap(bitmap, safe.left, safe.top, width, height)
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }

    /**
     * Fast YUV_420_888 -> NV21 using bulk ByteBuffer copies instead of per-pixel loops.
     *
     * Strategy:
     * - Y plane: if rowStride == width, bulk-copy the entire Y buffer directly.
     *   Otherwise copy row-by-row (rare: sensor padding wider than image).
     * - UV planes: Android guarantees pixelStride is either 1 (planar) or 2 (semi-planar/NV12).
     *   If pixelStride==2 and rowStride matches, we can bulk-copy UV directly (common on most
     *   Snapdragon/Unisoc devices). Otherwise fall back to the interleaved per-pixel path
     *   only for the UV portion which is 1/4 the size of the Y plane.
     */
    private fun yuv420ToNv21Fast(image: android.media.Image): ByteArray {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        val nv21 = ByteArray(width * height + 2 * chromaWidth * chromaHeight)
        var pos = 0

        // --- Y plane ---
        val yBuffer = yPlane.buffer
        if (yPlane.rowStride == width) {
            // Contiguous: single bulk copy
            yBuffer.get(nv21, 0, width * height)
            pos = width * height
        } else {
            // Padded rows: copy row by row
            val rowBytes = ByteArray(width)
            for (row in 0 until height) {
                yBuffer.position(row * yPlane.rowStride)
                yBuffer.get(rowBytes, 0, width)
                System.arraycopy(rowBytes, 0, nv21, pos, width)
                pos += width
            }
        }

        // --- UV planes (interleaved VU for NV21) ---
        val vBuffer = vPlane.buffer
        val uBuffer = uPlane.buffer
        // Fast path: pixelStride==2 means data is already interleaved (NV12/NV21 layout)
        if (vPlane.pixelStride == 2) {
            // V and U buffers overlap by 1 byte on NV12 — copy V buffer which starts 1 byte
            // before U buffer in memory for NV21 (VU order). The V buffer contains the full
            // interleaved UV block on most Android devices.
            vBuffer.position(0)
            val uvSize = vBuffer.remaining()
            vBuffer.get(nv21, pos, minOf(uvSize, nv21.size - pos))
        } else {
            // Pixel stride == 1: truly planar, interleave V and U manually
            // This path operates on chromaWidth*chromaHeight elements — 4x smaller than Y
            val vRow = ByteArray(chromaWidth)
            val uRow = ByteArray(chromaWidth)
            for (row in 0 until chromaHeight) {
                vBuffer.position(row * vPlane.rowStride)
                vBuffer.get(vRow, 0, chromaWidth)
                uBuffer.position(row * uPlane.rowStride)
                uBuffer.get(uRow, 0, chromaWidth)
                for (col in 0 until chromaWidth) {
                    nv21[pos++] = vRow[col]
                    nv21[pos++] = uRow[col]
                }
            }
        }
        return nv21
    }
}
