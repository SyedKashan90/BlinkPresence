package com.smartattendance.app.ui.screens.enroll

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.TokenStore
import com.smartattendance.app.face.FaceEmbedder
import com.smartattendance.app.face.FaceMatcher
import com.smartattendance.app.face.LivenessAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val LIVENESS_ONLY_SENTINEL = "LIVENESS_ONLY"

enum class EnrollStep { LOOK_AT_CAMERA, BLINK_NOW, PROCESSING, DONE, ERROR }

data class FaceEnrollUiState(
    val step: EnrollStep = EnrollStep.LOOK_AT_CAMERA,
    val message: String = "Center your face in the frame",
    val modelAvailable: Boolean = true,
)

class FaceEnrollViewModel(
    private val faceEmbedder: FaceEmbedder,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val liveness = LivenessAnalyzer()
    private val _state = MutableStateFlow(FaceEnrollUiState(modelAvailable = faceEmbedder.isAvailable))
    val state: StateFlow<FaceEnrollUiState> = _state.asStateFlow()

    private var captureTriggered = false

    fun onFrame(leftOpen: Float?, rightOpen: Float?, faceVisible: Boolean, onNeedsCapture: () -> Unit) {
        if (_state.value.step == EnrollStep.PROCESSING || _state.value.step == EnrollStep.DONE) return

        if (!faceVisible) {
            _state.update { it.copy(step = EnrollStep.LOOK_AT_CAMERA, message = "Center your face in the frame") }
            return
        }
        if (_state.value.step == EnrollStep.LOOK_AT_CAMERA) {
            _state.update { it.copy(step = EnrollStep.BLINK_NOW, message = "Blink naturally to confirm it's really you") }
        }
        if (liveness.onFrame(leftOpen, rightOpen) && !captureTriggered) {
            captureTriggered = true
            _state.update { it.copy(step = EnrollStep.PROCESSING, message = "Processing...") }
            onNeedsCapture()
        }
    }

    fun onCaptured(bitmap: Bitmap?) {
        viewModelScope.launch {
            if (bitmap == null) {
                failAndReset("Couldn't capture your face — try again")
                return@launch
            }
            if (!faceEmbedder.isAvailable) {
                // No bundled TFLite model: fall back to a liveness-only enrollment
                // so the rest of the app (QR scan, marking attendance) still works end-to-end.
                tokenStore.faceEmbedding = LIVENESS_ONLY_SENTINEL
                _state.update { it.copy(step = EnrollStep.DONE, message = "Enrolled (liveness-only mode — no embedding model bundled)") }
                return@launch
            }
            val embedding = faceEmbedder.embed(bitmap)
            if (embedding == null) {
                failAndReset("Face processing failed — try again")
                return@launch
            }
            tokenStore.faceEmbedding = FaceMatcher.encode(embedding)
            _state.update { it.copy(step = EnrollStep.DONE, message = "Face enrolled") }
        }
    }

    private fun failAndReset(message: String) {
        captureTriggered = false
        liveness.reset()
        _state.update { it.copy(step = EnrollStep.ERROR, message = message) }
    }

    fun retry() {
        captureTriggered = false
        liveness.reset()
        _state.update { it.copy(step = EnrollStep.LOOK_AT_CAMERA, message = "Center your face in the frame") }
    }
}
