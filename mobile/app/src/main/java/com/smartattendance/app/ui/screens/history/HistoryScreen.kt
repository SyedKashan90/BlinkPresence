package com.smartattendance.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartattendance.app.ServiceLocator
import com.smartattendance.app.data.local.CachedAttendanceEntity
import com.smartattendance.app.ui.components.Badge
import com.smartattendance.app.ui.components.Card
import com.smartattendance.app.ui.theme.Danger
import com.smartattendance.app.ui.theme.Success
import com.smartattendance.app.ui.theme.Warning

@Composable
fun HistoryScreen(services: ServiceLocator) {
    val viewModel: HistoryViewModel = viewModel(
        factory = viewModelFactory { initializer { HistoryViewModel(services.attendanceRepository) } }
    )
    val state by viewModel.state.collectAsState()
    val records by viewModel.records.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Attendance History", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            state.error?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }

        if (records.isEmpty() && !state.refreshing) {
            item { Text("No attendance records yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        items(records, key = { it.id }) { record -> HistoryRow(record) }
    }
}

@Composable
private fun HistoryRow(record: CachedAttendanceEntity) {
    val color = when (record.attendanceStatus) {
        "present" -> Success
        "late" -> Warning
        else -> Danger
    }
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(record.courseCode, fontWeight = FontWeight.Medium)
                Text(record.lectureDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    record.verificationMethod.replace("_", " "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Badge(text = record.attendanceStatus, color = color)
        }
    }
}
