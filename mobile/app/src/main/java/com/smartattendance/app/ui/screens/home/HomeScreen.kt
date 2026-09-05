package com.smartattendance.app.ui.screens.home

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
import com.smartattendance.app.data.remote.dto.CourseStatDto
import com.smartattendance.app.ui.components.Badge
import com.smartattendance.app.ui.components.Card
import com.smartattendance.app.ui.components.FullScreenLoading
import com.smartattendance.app.ui.components.PrimaryButton
import com.smartattendance.app.ui.components.StatTile
import com.smartattendance.app.ui.theme.Danger
import com.smartattendance.app.ui.theme.Secondary
import com.smartattendance.app.ui.theme.Success
import com.smartattendance.app.ui.theme.Warning
import kotlin.math.roundToInt

@Composable
fun HomeScreen(services: ServiceLocator, onScanQr: () -> Unit, onEnrollFace: () -> Unit) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(services.studentRepository, services.attendanceRepository, services.authRepository) }
        }
    )
    val state by viewModel.state.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Hi, ${state.fullName.ifBlank { "there" }}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Here's your attendance overview", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (pendingSync > 0) {
            item {
                Card {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pending sync", fontWeight = FontWeight.Medium)
                        Badge(text = "$pendingSync queued", color = Warning)
                    }
                    Text(
                        "Marked while offline — will sync automatically once you're back online.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (!state.isFaceEnrolled) {
            item {
                Card {
                    Text("Face not enrolled", fontWeight = FontWeight.Medium)
                    Text(
                        "Enroll your face once so attendance verification works.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    PrimaryButton(text = "Enroll now", onClick = onEnrollFace, containerColor = Secondary)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Overall attendance",
                    value = "${state.overallPercentage.roundToInt()}%",
                    modifier = Modifier.weight(1f),
                )
                StatTile(label = "Courses", value = "${state.courses.size}", modifier = Modifier.weight(1f))
            }
        }

        item {
            PrimaryButton(text = "Scan QR to Mark Attendance", onClick = onScanQr)
        }

        if (state.loading) {
            item { FullScreenLoading() }
        }

        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        }

        item {
            Text("My Courses", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
        }

        items(state.courses) { course -> CourseRow(course) }
    }
}

@Composable
private fun CourseRow(course: CourseStatDto) {
    val pct = course.attendancePercentage
    val color = when {
        pct >= 85 -> Success
        pct >= 75 -> Warning
        else -> Danger
    }
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(course.courseCode, fontWeight = FontWeight.Medium)
                Text(course.courseName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${course.attendedLectures}/${course.totalLectures} lectures",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Badge(text = "${pct.roundToInt()}%", color = color)
        }
    }
}
