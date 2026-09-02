package com.example.pinmind.core.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pinmind.core.notification.NotificationHelper
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.usecase.GetTaskByIdUseCase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OS-managed BroadcastReceiver handling geofence transition events from the Geofencing API.
 * Resistant to OEM background kills because the OS delivers the PendingIntent directly.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getTaskByIdUseCase: GetTaskByIdUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_EVENT = "com.example.pinmind.ACTION_GEOFENCE_EVENT"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence error: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val isTriggerTransition = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transitionType == Geofence.GEOFENCE_TRANSITION_DWELL

        if (isTriggerTransition) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
            val pendingResult = goAsync()

            receiverScope.launch {
                try {
                    for (geofence in triggeringGeofences) {
                        val taskId = geofence.requestId.toLongOrNull() ?: continue
                        val task = getTaskByIdUseCase(taskId).firstOrNull()

                        if (task != null && task.status == TaskStatus.ACTIVE) {
                            Log.d(TAG, "Firing reminder notification for task: ${task.title} (ID: ${task.id})")
                            notificationHelper.showTaskReminderNotification(task)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling geofence transition", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
