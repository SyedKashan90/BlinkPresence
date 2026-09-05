package com.smartattendance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.smartattendance.app.ui.LocalServices
import com.smartattendance.app.ui.navigation.SmartAttendanceNavGraph
import com.smartattendance.app.ui.theme.SmartAttendanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val services = (application as SmartAttendanceApp).services

        setContent {
            SmartAttendanceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalServices provides services) {
                        SmartAttendanceNavGraph(services)
                    }
                }
            }
        }
    }
}
