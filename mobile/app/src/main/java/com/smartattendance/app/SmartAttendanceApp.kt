package com.smartattendance.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.smartattendance.app.sync.AttendanceSyncWorker

class SmartAttendanceApp : Application() {

    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator.get(this)

        // Flush any offline-queued attendance marks as soon as we have a
        // network again, in addition to the app-launch attempt below.
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager?.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    AttendanceSyncWorker.enqueue(applicationContext)
                }
            },
        )

        AttendanceSyncWorker.enqueue(this)
    }
}
