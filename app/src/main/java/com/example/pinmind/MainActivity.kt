package com.example.pinmind

import android.Manifest
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.pinmind.core.location.LocationClient
import com.example.pinmind.core.notification.NotificationHelper
import com.example.pinmind.core.notification.NotificationPermissionHelper
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.repository.TaskRepository
import com.example.pinmind.presentation.navigation.PinMindNavGraph
import com.example.pinmind.ui.theme.PinMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var locationClient: LocationClient

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private var locationMonitoringJob: Job? = null
    private val triggeredTaskIds = mutableSetOf<Long>()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "PinMindApp/1.0 (${packageName})"
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationPermissionHelper.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            PinMindTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PinMindNavGraph()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationMonitoring()
    }

    override fun onPause() {
        super.onPause()
        stopLocationMonitoring()
    }

    /**
     * Starts listening to location updates while the activity is in ON_RESUME.
     * Ensures minimum power consumption by strictly running only in the foreground.
     */
    private fun startLocationMonitoring() {
        locationMonitoringJob?.cancel()
        locationMonitoringJob = lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting foreground location monitoring (ON_RESUME)")
                locationClient.getLocationUpdates(3000L).collect { userLoc ->
                    val activeTasks = taskRepository.getActiveTasksWithGeofence()
                    for (task in activeTasks) {
                        if (task.status != TaskStatus.ACTIVE) continue
                        val targetLoc = task.geoLocation ?: continue
                        val distResults = FloatArray(1)
                        Location.distanceBetween(
                            userLoc.latitude, userLoc.longitude,
                            targetLoc.latitude, targetLoc.longitude,
                            distResults
                        )
                        val dist = distResults[0]
                        if (dist <= targetLoc.radiusMeters) {
                            if (task.id !in triggeredTaskIds) {
                                triggeredTaskIds.add(task.id)
                                Log.d(
                                    TAG,
                                    "User entered radius for task ${task.id} ('${task.title}') [${dist.toInt()}m <= ${targetLoc.radiusMeters.toInt()}m]. Triggering notification!"
                                )
                                notificationHelper.showTaskReminderNotification(task)
                            }
                        } else {
                            // User left the radius; reset so it can trigger again on re-entry
                            triggeredTaskIds.remove(task.id)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Location monitoring job cancelled (ON_PAUSE)")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error in proximity location update monitor", e)
            }
        }
    }

    /**
     * Strictly cancels the location monitoring coroutine job in ON_PAUSE.
     * Rely 100% on the OS-managed GeofencingClient for background geofence alerts.
     */
    private fun stopLocationMonitoring() {
        locationMonitoringJob?.cancel()
        locationMonitoringJob = null
        Log.d(TAG, "Stopped location monitoring (ON_PAUSE) - Zero background GPS polling")
    }
}