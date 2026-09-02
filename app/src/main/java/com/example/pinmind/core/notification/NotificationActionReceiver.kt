package com.example.pinmind.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pinmind.domain.usecase.GetTaskByIdUseCase
import com.example.pinmind.domain.usecase.ToggleTaskStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver triggered by user actions on reminder notifications (e.g. "Mark as Done").
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getTaskByIdUseCase: GetTaskByIdUseCase

    @Inject
    lateinit var toggleTaskStatusUseCase: ToggleTaskStatusUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                            toggleTaskStatusUseCase(task)
                        }
                        if (notificationId != -1) {
                            notificationHelper.cancelNotification(notificationId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
