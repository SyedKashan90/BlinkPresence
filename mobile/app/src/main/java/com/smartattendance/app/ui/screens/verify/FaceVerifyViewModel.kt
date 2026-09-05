package com.smartattendance.app.ui.screens.verify

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.TokenStore
import com.smartattendance.app.data.repository.AttendanceRepository
import com.smartattendance.app.data.repository.MarkOutcome
import com.smartattendance.app.face.FaceEmbedder
import com.smartattendance.app.face.FaceMatcher
import com.smartattendance.app.face.LivenessAnalyzer
import com.smartattendance.app.ui.screens.enroll.LIVENESS_ONLY_SENTINEL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VerifyStep { LOOK_AT_CAMERA, BLINK_NOW, PROCESSING, SUCCESS, NO_MATCH, QUEUED_OFFLINE, ERROR }

data class FaceVerifyUiState(
    val step: VerifyStep = VerifyStep.LOOK_AT_CAMERA,
    val message: String = "Center your face in the frame",
)

class FaceVerifyViewModel(
    private val faceEmbedder: FaceEmbedder,
    private val tokenStore: TokenStore,
    private val attendanceRepository: AttendanceRepository,
    private val qrToken: String,
    private val courseCode: String,
) : ViewModel() {
    private val liveness = LivenessAnalyzer()
    private val _state = MutableStateFlow(FaceVerifyUiState())
    val state: StateFlow<FaceVerifyUiState> = _state.asStateFlow()
    private var captureTriggered = false

    fun onFrame(leftOpen: Float?, rightOpen: Float?, faceVisible: Boolean, onNeedsCapture: () -> Unit) {
        if (_state.value.step != VerifyStep.LOOK_AT_CAMERA && _state.value.step != VerifyStep.BLINK_NOW) return

        if (!faceVisible) {
            _state.update { it.copy(step = VerifyStep.LOOK_AT_CAMERA, message = "Center your face in the frame") }
            return
        }
        if (_state.value.step == VerifyStep.LOOK_AT_CAMERA) {
            _state.update { it.copy(step = VerifyStep.BLINK_NOW, message = "Blink naturally to verify liveness") }
        }
        if (liveness.onFrame(leftOpen, rightOpen) && !captureTriggered) {
            captureTriggered = true
            _state.update { it.copy(step = VerifyStep.PROCESSING, message = "Verifying...") }
            onNeedsCapture()
        }
    }

    fun onCaptured(bitmap: Bitmap?) {
        viewModelScope.launch {
            val enrolled = tokenStore.faceEmbedding
            if (bitmap == null) {
                retry("Couldn't capture your face — try again")
                return@launch
            }

            val (matches, score) = when {
                enrolled == null -> Pair(false, null)
                enrolled == LIVENESS_ONLY_SENTINEL -> Pair(true, null) // no embedding model bundled: liveness gate is the whole check
                !faceEmbedder.isAvailable -> Pair(true, null)
                else -> {
                    val candidate = faceEmbedder.embed(bitmap)
                    if (candidate != null) {
                        val sim = FaceMatcher.cosineSimilarity(candidate, FaceMatcher.decode(enrolled))
                        Pair(sim >= FaceMatcher.DEFAULT_THRESHOLD, sim)
                    } else {
                        Pair(false, null)
                    }
                }
            }

            if (!matches) {
                val scoreMsg = if (score != null) " (Match score: ${(score * 100).toInt()}%)" else ""
                _state.update { it.copy(step = VerifyStep.NO_MATCH, message = "Face didn't match your enrolled profile$scoreMsg") }
                return@launch
            }

            when (val outcome = attendanceRepository.markAttendance(qrToken, courseCode)) {
                is MarkOutcome.Success -> _state.update {
                    it.copy(step = VerifyStep.SUCCESS, message = "Attendance marked for $courseCode")
                }
                is MarkOutcome.Rejected -> _state.update {
                    it.copy(step = VerifyStep.ERROR, message = outcome.reason)
                }
                MarkOutcome.QueuedOffline -> _state.update {
                    it.copy(step = VerifyStep.QUEUED_OFFLINE, message = "Saved offline — will sync once you're back online")
                }
            }
        }
    }

    private fun retry(message: String) {
        captureTriggered = false
        liveness.reset()
        _state.update { it.copy(step = VerifyStep.ERROR, message = message) }
    }

    fun reset() {
        captureTriggered = false
        liveness.reset()
        _state.update { it.copy(step = VerifyStep.LOOK_AT_CAMERA, message = "Center your face in the frame") }
    }
}
