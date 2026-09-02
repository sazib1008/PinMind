package com.example.pinmind.core.location

import com.example.pinmind.domain.location.GeofenceController
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.Task
import com.example.pinmind.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Intelligent geofence scheduler enforcing the Android OS hard platform limit of 100 geofences per app.
 *
 * It sorts all active tasks by proximity to the user's current location, restricts registration
 * to the closest [MAX_ACTIVE_GEOFENCES] tasks (and within [MAX_RELEVANT_DISTANCE_METERS]), and
 * gracefully swaps out geofences when location changes.
 */
@Singleton
class GeofenceScheduler @Inject constructor(
    private val taskRepository: TaskRepository,
    private val geofenceController: GeofenceController,
    private val locationClient: LocationClient
) {

    companion object {
        /** Hard Android OS ceiling is 100 geofences per app; we cap active geofences to 50 for safety margin */
        const val MAX_ACTIVE_GEOFENCES = 50

        /** 20 kilometers maximum relevance radius for geofence pre-registration */
        const val MAX_RELEVANT_DISTANCE_METERS = 20_000.0

        /** Earth radius in meters */
        private const val EARTH_RADIUS_METERS = 6371000.0
    }

    /**
     * Evaluates all active tasks and registers geofences for the closest ones relative to the user's location.
     *
     * @param referenceLocation Optional user location. If null, fetches the current device location.
     * @return [Result] containing the count of registered geofences.
     */
    suspend fun scheduleClosestGeofences(referenceLocation: GeoLocation? = null): Result<Int> {
        return try {
            val userLoc = referenceLocation ?: locationClient.getCurrentLocation()

            // Fetch all active tasks from repository
            val activeTasks = taskRepository.getActiveTasksWithGeofence()


            if (activeTasks.isEmpty()) {
                geofenceController.removeAllGeofences()
                return Result.success(0)
            }

            val tasksToRegister: List<Task> = if (userLoc != null) {
                // Rank tasks by distance to user location and limit to tasks within 20km
                activeTasks
                    .map { task ->
                        val loc = task.geoLocation!!
                        val dist = calculateDistanceMeters(
                            lat1 = userLoc.latitude,
                            lon1 = userLoc.longitude,
                            lat2 = loc.latitude,
                            lon2 = loc.longitude
                        )
                        task to dist
                    }
                    .filter { it.second <= MAX_RELEVANT_DISTANCE_METERS }
                    .sortedBy { it.second }
                    .take(MAX_ACTIVE_GEOFENCES)
                    .map { it.first }
            } else {
                // If location is unknown, take the most recent tasks up to the limit
                activeTasks.take(MAX_ACTIVE_GEOFENCES)
            }

            // Remove all existing geofences and register the optimal candidate set
            geofenceController.removeAllGeofences()
            geofenceController.registerGeofences(tasksToRegister)

            Result.success(tasksToRegister.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculates the great-circle distance between two geographic coordinates using the Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
