package com.example.pinmind.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-registers active geofences after device reboot or app update.
 * Enforces platform constraint: OS wipes all registered geofences on device reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var geofenceScheduler: GeofenceScheduler

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Reboot/Update detected ($action). Rescheduling active geofences...")

            val pendingResult = goAsync()
            receiverScope.launch {
                try {
                    val result = geofenceScheduler.scheduleClosestGeofences()
                    result.onSuccess { count ->
                        Log.d(TAG, "Successfully restored $count geofences after reboot.")
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to restore geofences after reboot", error)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
