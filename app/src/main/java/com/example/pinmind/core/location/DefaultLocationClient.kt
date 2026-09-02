package com.example.pinmind.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.example.pinmind.domain.model.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Concrete implementation of [LocationClient] leveraging Google Play Services [FusedLocationProviderClient].
 */
class DefaultLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): GeoLocation? {
        if (!LocationPermissionHelper.hasForegroundPermission(context)) {
            return null
        }

        return try {
            val last = client.lastLocation.await()
            if (last != null) {
                return GeoLocation(
                    latitude = last.latitude,
                    longitude = last.longitude,
                    radiusMeters = 100f
                )
            }

            val location = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            location?.let {
                GeoLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    radiusMeters = 100f
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(intervalMs: Long): Flow<GeoLocation> = callbackFlow {
        if (!LocationPermissionHelper.hasForegroundPermission(context)) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let { location ->
                    trySend(
                        GeoLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            radiusMeters = 100f
                        )
                    )
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }
}
