package com.smartattendance.app.ui.screens.enroll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartattendance.app.ServiceLocator
import com.smartattendance.app.ui.components.FaceCameraPreview
import com.smartattendance.app.ui.components.FaceCaptureController
import com.smartattendance.app.ui.components.PrimaryButton
import com.smartattendance.app.ui.components.RequireCameraPermission
import com.smartattendance.app.ui.theme.Danger
import com.smartattendance.app.ui.theme.Primary
import com.smartattendance.app.ui.theme.Success

@Composable
fun FaceEnrollScreen(services: ServiceLocator, onDone: () -> Unit, onCancel: () -> Unit) {
    val viewModel: FaceEnrollViewModel = viewModel(
        factory = viewModelFactory { initializer { FaceEnrollViewModel(services.faceEmbedder, services.tokenStore) } }
    )
    val state by viewModel.state.collectAsState()
    val controller = remember { FaceCaptureController() }

    androidx.compose.runtime.LaunchedEffect(state.step) {
        if (state.step == EnrollStep.DONE) {
            onDone()
        }
    }

    RequireCameraPermission(rationale = "Camera access is needed to enroll your face for attendance verification.") {
        Box(Modifier.fillMaxSize()) {
            FaceCameraPreview(
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                onFrame = { face ->
                    viewModel.onFrame(
                        leftOpen = face?.leftEyeOpenProbability,
                        rightOpen = face?.rightEyeOpenProbability,
                        faceVisible = face != null,
                        onNeedsCapture = { controller.requestCapture { bitmap -> viewModel.onCaptured(bitmap) } },
                    )
                },
            )

            val ringColor = when (state.step) {
                EnrollStep.ERROR -> Danger
                EnrollStep.PROCESSING -> Success
                else -> Color.White
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .border(4.dp, ringColor, CircleShape),
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(20.dp),
            ) {
                Text(state.message, color = Color.White, fontWeight = FontWeight.Medium)
                if (!state.modelAvailable) {
                    Text(
                        "Embedding model not bundled — running liveness-only mode.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (state.step == EnrollStep.ERROR) {
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
                    PrimaryButton(text = "Try again", onClick = viewModel::retry, containerColor = Primary)
                }
            }
        }
    }
}
