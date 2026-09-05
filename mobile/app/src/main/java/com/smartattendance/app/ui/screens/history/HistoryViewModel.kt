package com.smartattendance.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartattendance.app.data.local.CachedAttendanceEntity
import com.smartattendance.app.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(val refreshing: Boolean = true, val error: String? = null)

class HistoryViewModel(private val attendanceRepository: AttendanceRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    val records: StateFlow<List<CachedAttendanceEntity>> = attendanceRepository.observeCachedHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            val result = attendanceRepository.refreshHistory()
            val error = (result as? com.smartattendance.app.data.remote.ApiResult.Error)?.message
                ?: (result as? com.smartattendance.app.data.remote.ApiResult.NetworkUnavailable)?.let { "Offline — showing cached history." }
            _state.update { it.copy(refreshing = false, error = error) }
        }
    }
}
