package com.smartattendance.app.qr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * QR Scanner Module (TRD 5): decodes the lecture's signed QR token straight
 * off the Y (luma) plane — no bitmap conversion needed, which keeps this
 * fast enough to run on every analyzer frame per the <2s QR-scan budget.
 *
 * Performance: TRY_HARDER is intentionally omitted — it forces exhaustive
 * multi-rotation scanning on every frame, which is prohibitively slow on
 * budget hardware. Restricting to QR_CODE format skips all barcode/PDF417
 * attempts, cutting per-frame decode time significantly.
 */
class QrAnalyzer(
    private val onDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            )
        )
    }

    @Volatile private var paused = false

    fun pause() { paused = true }
    fun resume() { paused = false }

    override fun analyze(imageProxy: ImageProxy) {
        if (paused) {
            imageProxy.close()
            return
        }
        try {
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val data = ByteArray(yBuffer.remaining())
            yBuffer.get(data)

            // rowStride may exceed width due to sensor padding — PlanarYUVLuminanceSource
            // needs the real stride as dataWidth or it reads the image skewed.
            val source = PlanarYUVLuminanceSource(
                data, yPlane.rowStride, imageProxy.height,
                0, 0, imageProxy.width, imageProxy.height, false,
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(bitmap)
            onDecoded(result.text)
        } catch (e: NotFoundException) {
            // No QR in this frame — normal, keep scanning.
        } catch (e: Exception) {
            // Decoding hiccup on a single frame; ignore and keep scanning.
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }
}
