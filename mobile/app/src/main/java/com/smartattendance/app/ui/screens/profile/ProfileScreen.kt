package com.smartattendance.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartattendance.app.ServiceLocator
import com.smartattendance.app.ui.components.Badge
import com.smartattendance.app.ui.components.Card
import com.smartattendance.app.ui.components.FullScreenLoading
import com.smartattendance.app.ui.components.PrimaryButton
import com.smartattendance.app.ui.theme.Danger
import com.smartattendance.app.ui.theme.Success

@Composable
fun ProfileScreen(services: ServiceLocator, onLoggedOut: () -> Unit) {
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory { initializer { ProfileViewModel(services.studentRepository, services.authRepository) } }
    )
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.load(context) }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        if (state.loading) {
            FullScreenLoading()
        }

        state.student?.let { student ->
            Card {
                Text(student.fullName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(student.registrationNumber, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                ProfileField("Email", student.email)
                ProfileField("Department", student.departmentDetail?.departmentName ?: "—")
                ProfileField("Semester", student.semester.toString())
                if (student.phone.isNotBlank()) ProfileField("Phone", student.phone)
            }
        }

        Card {
            Row2("Device binding") {
                Badge(
                    text = if (state.deviceRegistered) "Registered" else "Pending",
                    color = if (state.deviceRegistered) Success else Danger,
                )
            }
            Text(
                "This device is bound to your account to deter attendance sharing.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        PrimaryButton(text = "Log out", onClick = viewModel::logout, containerColor = Danger)
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Row2(label) { Text(value, fontSize = 13.sp) }
}

@Composable
private fun Row2(label: String, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
