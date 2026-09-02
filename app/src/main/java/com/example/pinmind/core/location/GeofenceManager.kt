package com.example.pinmind.core.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.pinmind.core.notification.NotificationHelper
import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.GeofenceTransitionType
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.model.TaskStatus
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages registering and unregistering geofences with Google Play Services [GeofencingClient].
 *
 * Enforces platform constraints:
 * - OS-managed [PendingIntent] / [GeofenceBroadcastReceiver] (resistant to OEM background service kills).
 * - Default to ENTER + DWELL transitions with configurable dwell time to avoid false drive-by triggers.
 * - Immediate proximity evaluation on registration so notifications fire when already inside radius.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationClient: LocationClient,
    private val notificationHelper: NotificationHelper
) : GeofenceController {

    companion object {
        private const val TAG = "GeofenceManager"
    }

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Registers a geofence for a single task.
     */
    override suspend fun registerGeofence(task: Task): Result<Unit> {
        val location = task.geoLocation ?: return Result.failure(
            IllegalArgumentException("Cannot register geofence for task without location")
        )

        return registerGeofences(listOf(task))
    }

    /**
     * Registers geofences for a batch of tasks.
     */
    @SuppressLint("MissingPermission")
    override suspend fun registerGeofences(tasks: List<Task>): Result<Unit> {
        if (!LocationPermissionHelper.hasForegroundPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        val geofenceTasks = tasks.filter { it.hasActiveGeofence }
        if (geofenceTasks.isEmpty()) {
            return Result.success(Unit)
        }

        val geofenceList = geofenceTasks.mapNotNull { task ->
            val loc = task.geoLocation ?: return@mapNotNull null

            val transitionTypes = when (task.transitionType) {
                GeofenceTransitionType.ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
                GeofenceTransitionType.EXIT -> Geofence.GEOFENCE_TRANSITION_EXIT
                GeofenceTransitionType.DWELL -> Geofence.GEOFENCE_TRANSITION_DWELL
                GeofenceTransitionType.ENTER_OR_DWELL -> Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
            }

            Geofence.Builder()
                .setRequestId(task.id.toString())
                .setCircularRegion(loc.latitude, loc.longitude, loc.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionTypes)
                .setLoiteringDelay(task.dwellTimeSeconds * 1000)
                .setNotificationResponsiveness(5000) // 5 seconds responsiveness
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.d(TAG, "Successfully registered ${geofenceList.size} geofences with Google Play Services")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofences with Google Play Services", e)
        }

        // Proactive Proximity Check:
        // If the user's current device location is already inside any of the registered tasks' radius,
        // trigger the reminder notification immediately.
        checkImmediateProximity(geofenceTasks)

        return Result.success(Unit)
    }

    private suspend fun checkImmediateProximity(tasks: List<Task>) {
        try {
            val currentLoc = locationClient.getCurrentLocation()
            if (currentLoc == null) {
                Log.w(TAG, "Cannot check immediate proximity: Current device location is null")
                return
            }

            for (task in tasks) {
                if (task.status != TaskStatus.ACTIVE) continue
                val targetLoc = task.geoLocation ?: continue
                val distanceResults = FloatArray(1)
                android.location.Location.distanceBetween(
                    currentLoc.latitude, currentLoc.longitude,
                    targetLoc.latitude, targetLoc.longitude,
                    distanceResults
                )
                val distanceMeters = distanceResults[0]
                Log.d(
                    TAG,
                    "Task ${task.id} ('${task.title}'): Distance is ${distanceMeters.toInt()}m, target radius is ${targetLoc.radiusMeters.toInt()}m"
                )

                if (distanceMeters <= targetLoc.radiusMeters) {
                    Log.d(TAG, "Device is ALREADY inside radius for task ${task.id}. Firing notification!")
                    notificationHelper.showTaskReminderNotification(task)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking immediate proximity for registered geofences", e)
        }
    }

    /**
     * Unregisters the geofence for a specific task ID.
     */
    override suspend fun removeGeofence(taskId: Long): Result<Unit> {
        return try {
            geofencingClient.removeGeofences(listOf(taskId.toString())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unregisters geofences for a list of task IDs.
     */
    override suspend fun removeGeofences(taskIds: List<Long>): Result<Unit> {
        if (taskIds.isEmpty()) return Result.success(Unit)
        return try {
            geofencingClient.removeGeofences(taskIds.map { it.toString() }).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears all registered geofences.
     */
    override suspend fun removeAllGeofences(): Result<Unit> {
        return try {
            geofencingClient.removeGeofences(geofencePendingIntent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

