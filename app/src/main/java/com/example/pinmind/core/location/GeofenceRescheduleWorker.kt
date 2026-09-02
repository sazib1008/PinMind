package com.example.pinmind.core.location

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker to re-evaluate and synchronize geofences based on user location.
 * Helps ensure geofences remain aligned with the 100-geofence limit even when the app is in the background.
 */
class GeofenceRescheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GeofenceWorkerEntryPoint {
        fun geofenceScheduler(): GeofenceScheduler
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                GeofenceWorkerEntryPoint::class.java
            )
            val scheduler = entryPoint.geofenceScheduler()
            val result = scheduler.scheduleClosestGeofences()
            result.onSuccess { count ->
                Log.d(TAG, "Periodic reschedule synced $count geofences.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic geofence reschedule failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "GeofenceWorker"
        private const val WORK_NAME = "pinmind_periodic_geofence_reschedule"

        /**
         * Enqueues periodic geofence synchronization work (every 6 hours).
         */
        fun enqueuePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<GeofenceRescheduleWorker>(
                6, TimeUnit.HOURS,
                1, TimeUnit.HOURS // flex interval
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
