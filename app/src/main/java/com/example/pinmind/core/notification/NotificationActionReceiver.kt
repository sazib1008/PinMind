package com.example.pinmind.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.TaskStatus
import com.example.pinmind.domain.repository.TaskRepository
import com.example.pinmind.domain.usecase.GetTaskByIdUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver triggered by user actions on reminder notifications (e.g. "Mark as Complete").
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getTaskByIdUseCase: GetTaskByIdUseCase

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var geofenceController: GeofenceController

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "NotificationAction"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == NotificationHelper.ACTION_MARK_DONE) {
            val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
            val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)

            if (taskId > 0L) {
                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        val task = getTaskByIdUseCase(taskId).firstOrNull()
                        if (task != null) {
                            Log.d(TAG, "Marking task ${task.id} as COMPLETED from notification action")
                            val completedTask = task.copy(
                                status = TaskStatus.COMPLETED,
                                completedAt = System.currentTimeMillis()
                            )
                            taskRepository.updateTask(completedTask)
                            geofenceController.removeGeofence(task.id)
                        }

                        if (notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to mark task as completed from notification", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
