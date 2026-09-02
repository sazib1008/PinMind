package com.example.pinmind

import android.app.Application
import com.example.pinmind.core.location.GeofenceRescheduleWorker
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for PinMind, initializes Hilt dependency injection and background schedulers.
 */
@HiltAndroidApp
class PinMindApp : Application() {

    override fun onCreate() {
        super.onCreate()
        GeofenceRescheduleWorker.enqueuePeriodicWork(this)
    }
}

