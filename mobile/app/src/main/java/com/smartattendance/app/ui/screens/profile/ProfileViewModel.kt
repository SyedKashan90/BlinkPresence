package com.smartattendance.app.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.remote.ApiResult
import com.smartattendance.app.data.remote.dto.StudentDto
import com.smartattendance.app.data.repository.AuthRepository
import com.smartattendance.app.data.repository.StudentRepository
import com.smartattendance.app.util.DeviceIdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val student: StudentDto? = null,
    val deviceRegistered: Boolean = false,
    val loggedOut: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(
    private val studentRepository: StudentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val profile = studentRepository.profile()) {
                is ApiResult.Success -> _state.update { it.copy(student = profile.data) }
                is ApiResult.Error -> _state.update { it.copy(error = profile.message) }
                ApiResult.NetworkUnavailable -> _state.update { it.copy(error = "Offline") }
            }

            val deviceId = DeviceIdProvider.get(context)
            when (val devices = studentRepository.registeredDevices()) {
                is ApiResult.Success -> {
                    val registered = devices.data.any { it.deviceId == deviceId }
                    _state.update { it.copy(loading = false, deviceRegistered = registered) }
                    if (!registered) registerThisDevice(context)
                }
                else -> _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun registerThisDevice(context: Context) {
        viewModelScope.launch {
            val deviceId = DeviceIdProvider.get(context)
            val name = DeviceIdProvider.displayName()
            if (studentRepository.registerDevice(deviceId, name) is ApiResult.Success) {
                _state.update { it.copy(deviceRegistered = true) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.update { it.copy(loggedOut = true) }
        }
    }
}
