package com.smartattendance.app.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smartattendance.app.face.FaceDetectorAnalyzer
import com.smartattendance.app.face.FrameFace
import com.smartattendance.app.util.ImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Bridges the composable's capture request into the analyzer's synchronous frame callback. */
class FaceCaptureController {
    internal val captureRequested = AtomicBoolean(false)
    internal var onCaptured: ((Bitmap?) -> Unit)? = null

    fun requestCapture(onResult: (Bitmap?) -> Unit) {
        onCaptured = onResult
        captureRequested.set(true)
    }
}

@Composable
fun FaceCameraPreview(
    modifier: Modifier = Modifier,
    controller: FaceCaptureController,
    onFrame: (FrameFace?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null
        var previewUseCase: Preview? = null
        var analysisUseCase: ImageAnalysis? = null
        // Frame throttle: forward every 3rd frame to the ViewModel to reduce
        // excessive StateFlow updates during blink detection (30fps → ~10fps ViewModel updates).
        var frameCount = 0

        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            previewUseCase = preview
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysisUseCase = analysis

            analysis.setAnalyzer(executor, FaceDetectorAnalyzer(executor) { face, imageProxy ->
                // --- Capture path: extract bitmap while imageProxy is still open ---
                var capturedBitmap: Bitmap? = null
                if (controller.captureRequested.get() && face != null) {
                    controller.captureRequested.set(false)
                    capturedBitmap = runCatching {
                        val upright = ImageUtils.imageProxyToUprightBitmap(imageProxy) ?: return@runCatching null
                        val box = Rect(face.boundingBoxLeft, face.boundingBoxTop, face.boundingBoxRight, face.boundingBoxBottom)
                        ImageUtils.cropAndResize(upright, box, 112)
                    }.onFailure { Log.w("FaceCamera", "capture failed", it) }.getOrNull()
                }

                // --- Caller-owned close: always close AFTER bitmap extraction is done ---
                imageProxy.close()

                // --- Deliver capture result if we extracted a bitmap ---
                if (capturedBitmap != null) {
                    controller.onCaptured?.invoke(capturedBitmap)
                    controller.onCaptured = null
                }

                // --- Frame throttle: only forward every 3rd frame to ViewModel ---
                frameCount++
                if (frameCount % 3 == 0) {
                    onFrame(face)
                }
            })

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis,
                )
            } catch (e: Exception) {
                Log.e("FaceCamera", "bind failed", e)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching {
                val provider = cameraProvider
                val pUseCase = previewUseCase
                val aUseCase = analysisUseCase
                if (provider != null && pUseCase != null && aUseCase != null) {
                    provider.unbind(pUseCase, aUseCase)
                }
            }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())
}
