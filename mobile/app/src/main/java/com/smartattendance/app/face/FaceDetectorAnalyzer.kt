package com.smartattendance.app.face

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executor

data class FrameFace(
    val boundingBoxLeft: Int,
    val boundingBoxTop: Int,
    val boundingBoxRight: Int,
    val boundingBoxBottom: Int,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val headEulerAngleY: Float,
    val rotationDegrees: Int,
)

/**
 * FR-06/FR-07: face detection + the signal LivenessAnalyzer uses for blink-based liveness.
 *
 * THREADING: [callbackExecutor] is the same single-thread executor used by ImageAnalysis.
 * By passing it to addOnSuccessListener/addOnFailureListener, ML Kit invokes [onResult]
 * on the analysis background thread instead of the main thread. This is critical because
 * [onResult] may perform heavy bitmap extraction (imageProxyToUprightBitmap) which would
 * otherwise block the UI thread and cause visible freeze/ANR.
 *
 * IMAGEPROXY LIFECYCLE: owned by the CALLER (FaceCamera.kt). This analyzer does NOT close
 * the imageProxy — the caller closes it after bitmap extraction is complete.
 */
class FaceDetectorAnalyzer(
    private val callbackExecutor: Executor,
    private val onResult: (FrameFace?, ImageProxy) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.25f)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)

        try {
            detector.process(input)
                // Pass callbackExecutor so callbacks run on the analysis thread, not main thread.
                .addOnSuccessListener(callbackExecutor) { faces: List<Face> ->
                    val best = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (best == null) {
                        onResult(null, imageProxy)
                    } else {
                        onResult(
                            FrameFace(
                                boundingBoxLeft = best.boundingBox.left,
                                boundingBoxTop = best.boundingBox.top,
                                boundingBoxRight = best.boundingBox.right,
                                boundingBoxBottom = best.boundingBox.bottom,
                                leftEyeOpenProbability = best.leftEyeOpenProbability,
                                rightEyeOpenProbability = best.rightEyeOpenProbability,
                                headEulerAngleY = best.headEulerAngleY,
                                rotationDegrees = rotation,
                            ),
                            imageProxy,
                        )
                    }
                }
                .addOnFailureListener(callbackExecutor) {
                    onResult(null, imageProxy)
                }
        } catch (e: Exception) {
            onResult(null, imageProxy)
        }
        // NOTE: No addOnCompleteListener — imageProxy.close() is the caller's responsibility.
    }
}

