package com.smartattendance.app.ui.screens.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartattendance.app.ServiceLocator
import com.smartattendance.app.qr.QrAnalyzer
import com.smartattendance.app.ui.components.RequireCameraPermission
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(services: ServiceLocator, onValidated: (token: String, courseCode: String) -> Unit) {
    val viewModel: QrScanViewModel = viewModel(
        factory = viewModelFactory { initializer { QrScanViewModel(services.attendanceRepository) } }
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.validated) {
        state.validated?.let { (token, courseCode) -> onValidated(token, courseCode) }
    }

    RequireCameraPermission(rationale = "Camera access is needed to scan the lecture's QR code.") {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val executor = remember { Executors.newSingleThreadExecutor() }
        val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

        DisposableEffect(Unit) {
            val future = ProcessCameraProvider.getInstance(context)
            var cameraProvider: ProcessCameraProvider? = null
            var previewUseCase: Preview? = null
            var analysisUseCase: ImageAnalysis? = null

            future.addListener({
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                previewUseCase = preview
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysisUseCase = analysis
                analysis.setAnalyzer(executor, QrAnalyzer { token -> viewModel.onDecoded(token) })
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(context))

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

        Box(Modifier.fillMaxSize()) {
            androidx.compose.ui.viewinterop.AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .border(3.dp, Color.White, RoundedCornerShape(20.dp)),
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(20.dp),
            ) {
                Text("Point your camera at the lecture's QR code", color = Color.White)
                if (state.busy) {
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                }
                state.error?.let {
                    Text(it, color = Color(0xFFFCA5A5), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
