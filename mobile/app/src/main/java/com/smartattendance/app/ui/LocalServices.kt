package com.smartattendance.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.smartattendance.app.ServiceLocator

val LocalServices = staticCompositionLocalOf<ServiceLocator> {
    error("ServiceLocator not provided — wrap content in CompositionLocalProvider(LocalServices provides ...)")
}
