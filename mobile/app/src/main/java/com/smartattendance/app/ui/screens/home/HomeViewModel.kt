package com.smartattendance.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.remote.ApiResult
import com.smartattendance.app.data.remote.dto.CourseStatDto
import com.smartattendance.app.data.repository.AttendanceRepository
import com.smartattendance.app.data.repository.AuthRepository
import com.smartattendance.app.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val fullName: String = "",
    val overallPercentage: Double = 0.0,
    val courses: List<CourseStatDto> = emptyList(),
    val isFaceEnrolled: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState(fullName = authRepository.cachedFullName ?: "Student"))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val pendingSyncCount: StateFlow<Int> = attendanceRepository.observePendingSyncCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, isFaceEnrolled = authRepository.isFaceEnrolled) }
            when (val result = studentRepository.statistics()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        overallPercentage = result.data.overallAttendancePercentage,
                        courses = result.data.courses,
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
                ApiResult.NetworkUnavailable -> _state.update {
                    it.copy(loading = false, error = "Offline — showing cached data where available.")
                }
            }
        }
    }
}
