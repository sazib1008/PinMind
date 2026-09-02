package com.example.pinmind.core.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility for verifying and managing Android 13+ (API 33+) POST_NOTIFICATIONS runtime permission.
 */
object NotificationPermissionHelper {

    /**
     * Checks whether POST_NOTIFICATIONS permission is granted on Android 13+ (API 33+).
     * On Android 12 and below, notifications do not require runtime permission and returns true.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
