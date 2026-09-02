package com.example.pinmind.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Helper class for checking Android location permission state.
 * Strictly adheres to the 2-step permission flow on Android 10+ (API 29+).
 */
object LocationPermissionHelper {

    /**
     * Checks whether foreground location (FINE or COARSE) is granted.
     */
    fun hasForegroundPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Checks whether background location (ACCESS_BACKGROUND_LOCATION) is granted.
     * On Android 9 and lower, background location is automatically included with foreground permission.
     */
    fun hasBackgroundPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            hasForegroundPermission(context)
        }
    }

    /**
     * Determines the current 3-state permission state for the application.
     */
    fun getPermissionState(context: Context): LocationPermissionState {
        val foreground = hasForegroundPermission(context)
        val background = hasBackgroundPermission(context)

        return when {
            background && foreground -> LocationPermissionState.GrantedAllTime
            foreground -> LocationPermissionState.ForegroundOnly
            else -> LocationPermissionState.Denied
        }
    }
}
