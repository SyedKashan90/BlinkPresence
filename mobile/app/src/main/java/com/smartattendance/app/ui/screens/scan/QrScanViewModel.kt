package com.smartattendance.app.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.remote.ApiResult
import com.smartattendance.app.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QrScanUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val validated: Pair<String, String>? = null, // token to courseCode
)

class QrScanViewModel(private val attendanceRepository: AttendanceRepository) : ViewModel() {
    private val _state = MutableStateFlow(QrScanUiState())
    val state: StateFlow<QrScanUiState> = _state.asStateFlow()

    private var lastToken: String? = null

    fun onDecoded(token: String) {
        if (_state.value.busy || token == lastToken) return
        lastToken = token
        _state.update { it.copy(busy = true, error = null) }

        viewModelScope.launch {
            when (val result = attendanceRepository.validateQr(token)) {
                is ApiResult.Success -> {
                    val body = result.data
                    if (body.valid) {
                        _state.update { it.copy(busy = false, validated = token to (body.courseCode ?: "")) }
                    } else {
                        _state.update { it.copy(busy = false, error = body.detail ?: "This QR code is no longer valid.") }
                        allowRetryAfterDelay()
                    }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(busy = false, error = result.message) }
                    allowRetryAfterDelay()
                }
                ApiResult.NetworkUnavailable -> {
                    _state.update { it.copy(busy = false, error = "You're offline — connect to validate the session before scanning.") }
                    allowRetryAfterDelay()
                }
            }
        }
    }

    private fun allowRetryAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            lastToken = null
        }
    }
}
