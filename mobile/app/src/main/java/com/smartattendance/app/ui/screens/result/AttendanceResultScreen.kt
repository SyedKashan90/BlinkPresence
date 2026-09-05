package com.smartattendance.app.ui.screens.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartattendance.app.ui.components.PrimaryButton
import com.smartattendance.app.ui.theme.Danger
import com.smartattendance.app.ui.theme.Success
import com.smartattendance.app.ui.theme.Warning

@Composable
fun AttendanceResultScreen(outcome: String, message: String, onDone: () -> Unit) {
    val (icon, color, title) = when (outcome) {
        "success" -> Triple(Icons.Filled.CheckCircle, Success, "Attendance Marked")
        "queued" -> Triple(Icons.Filled.CloudOff, Warning, "Saved Offline")
        else -> Triple(Icons.Filled.Error, Danger, "Couldn't Mark Attendance")
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(88.dp).background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(48.dp))
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 20.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
            )
            PrimaryButton(text = "Back to Home", onClick = onDone, containerColor = color)
        }
    }
}
