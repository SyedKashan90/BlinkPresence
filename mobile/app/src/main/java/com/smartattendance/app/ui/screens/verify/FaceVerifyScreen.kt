package com.smartattendance.app.ui.screens.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.smartattendance.app.ui.theme.Success
import com.smartattendance.app.ui.theme.Warning

@Composable
fun FaceVerifyScreen(
    services: ServiceLocator,
    qrToken: String,
    courseCode: String,
    onResult: (outcome: String, message: String) -> Unit,
) {
    val viewModel: FaceVerifyViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                FaceVerifyViewModel(services.faceEmbedder, services.tokenStore, services.attendanceRepository, qrToken, courseCode)
            }
        }
    )
    val state by viewModel.state.collectAsState()
    val controller = remember { FaceCaptureController() }

    LaunchedEffect(state.step) {
        when (state.step) {
            VerifyStep.SUCCESS -> onResult("success", state.message)
            VerifyStep.QUEUED_OFFLINE -> onResult("queued", state.message)
            else -> Unit
        }
    }

    RequireCameraPermission(rationale = "Camera access is needed to verify your face for attendance.") {
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
                VerifyStep.NO_MATCH, VerifyStep.ERROR -> Danger
                VerifyStep.PROCESSING -> Warning
                else -> Color.White
            }
            Box(Modifier.align(Alignment.Center).size(260.dp).border(4.dp, ringColor, CircleShape))

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(20.dp),
            ) {
                Text(state.message, color = Color.White, fontWeight = FontWeight.Medium)
                if (state.step == VerifyStep.NO_MATCH || state.step == VerifyStep.ERROR) {
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
                    PrimaryButton(text = "Try again", onClick = viewModel::reset)
                }
            }
        }
    }
}
