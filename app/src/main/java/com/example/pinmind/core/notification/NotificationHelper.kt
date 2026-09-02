package com.example.pinmind.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pinmind.MainActivity
import com.example.pinmind.R
import com.example.pinmind.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles creation of notification channels and posting rich task reminder notifications.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "pinmind_task_reminders"
        const val ACTION_MARK_DONE = "com.example.pinmind.ACTION_MARK_DONE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        val VIBRATION_PATTERN = longArrayOf(0, 300, 200, 300)
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_location)
            val descriptionText = context.getString(R.string.notif_channel_location_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setSound(defaultSoundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds and posts a rich heads-up notification for a geofence trigger.
     */
    fun showTaskGeofenceNotification(
        taskId: Long,
        title: String,
        description: String,
        category: String,
        locationName: String
    ) {
        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
            return
        }

        val notificationId = taskId.toInt()
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Content Intent: Tap on body launches MainActivity with EXTRA_TASK_ID
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Button: "Mark as Complete" intent pointing to NotificationActionReceiver
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (description.isNotBlank()) {
            "📍 $locationName • $description"
        } else {
            "📍 Arrived at $locationName"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location_pin)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyText)
                    .setSummaryText(category.ifBlank { "Reminder" })
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(VIBRATION_PATTERN)
            .setLights(Color.BLUE, 500, 500)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.notif_action_complete),
                donePendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Shows a reminder notification for the triggered [Task], delegating to [showTaskGeofenceNotification].
     */
    fun showTaskReminderNotification(task: Task) {
        val locationText = task.geoLocation?.locationName?.ifBlank { null }
            ?: task.geoLocation?.address
            ?: "Nearby"

        showTaskGeofenceNotification(
            taskId = task.id,
            title = task.title,
            description = task.description,
            category = task.category,
            locationName = locationText
        )
    }

    /**
     * Cancels a notification by its ID.
     */
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}

