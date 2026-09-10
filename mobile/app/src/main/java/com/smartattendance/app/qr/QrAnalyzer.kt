package com.smartattendance.app.qr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * QR Scanner Module (TRD 5): High-performance ML Kit Barcode Scanner with fallback to ZXing.
 * Uses InputImage.fromMediaImage to handle sensor rotation (0, 90, 180, 270 deg) automatically.
 */
class QrAnalyzer(
    private val onDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    private val zxingReader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    @Volatile private var paused = false
    @Volatile private var isProcessing = false

    fun pause() { paused = true }
    fun resume() { paused = false }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (paused || isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            isProcessing = true
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val qrCode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
                    if (!qrCode.isNullOrBlank()) {
                        onDecoded(qrCode)
                    } else {
                        tryDecodeWithZxing(imageProxy)
                    }
                }
                .addOnFailureListener {
                    tryDecodeWithZxing(imageProxy)
                }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            tryDecodeWithZxing(imageProxy)
            imageProxy.close()
        }
    }

    private fun tryDecodeWithZxing(imageProxy: ImageProxy) {
        try {
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer
            val data = ByteArray(yBuffer.remaining())
            yBuffer.get(data)

            var source: com.google.zxing.LuminanceSource = PlanarYUVLuminanceSource(
                data, yPlane.rowStride, imageProxy.height,
                0, 0, imageProxy.width, imageProxy.height, false,
            )
            val rotation = imageProxy.imageInfo.rotationDegrees
            if (rotation == 90 || rotation == 270) {
                source = source.rotateCounterClockwise()
            }

            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = zxingReader.decodeWithState(bitmap)
            if (!result.text.isNullOrBlank()) {
                onDecoded(result.text)
            }
        } catch (_: Exception) {
            // Frame contained no valid QR barcode
        } finally {
            zxingReader.reset()
        }
    }
}

