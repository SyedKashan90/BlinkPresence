package com.smartattendance.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

/** Stable per-install identifier used for the account/device binding in RegisteredDevice (BSD Section 14). */
object DeviceIdProvider {
    @SuppressLint("HardwareIds")
    fun get(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"

    fun displayName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
