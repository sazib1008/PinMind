package com.example.pinmind

import android.app.Application
import android.content.Context
import com.example.pinmind.core.location.GeofenceRescheduleWorker
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Main Application class for PinMind, initializes Hilt dependency injection, osmdroid, and background schedulers.
 */
@HiltAndroidApp
class PinMindApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize OpenStreetMap osmdroid configuration
        val userAgent = "PinMindApp/1.0 (${packageName})"
        val sharedPrefs = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, sharedPrefs)
        Configuration.getInstance().userAgentValue = userAgent

        val basePath = File(cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        Configuration.getInstance().osmdroidBasePath = basePath
        Configuration.getInstance().osmdroidTileCache = tileCache

        // Clean stale/blocked 403 tile cache on startup so fresh tiles are downloaded
        try {
            if (basePath.exists()) {
                basePath.deleteRecursively()
            }
            externalCacheDir?.let { ext ->
                val extBase = File(ext, "osmdroid")
                if (extBase.exists()) {
                    extBase.deleteRecursively()
                }
            }
        } catch (_: Exception) {}

        GeofenceRescheduleWorker.enqueuePeriodicWork(this)
    }
}



