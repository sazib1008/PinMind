package com.example.pinmind.core.location

import com.example.pinmind.domain.model.GeoLocation
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining location retrieval operations.
 */
interface LocationClient {

    /**
     * Retrieves the single most accurate last-known or current location snapshot.
     */
    suspend fun getCurrentLocation(): GeoLocation?

    /**
     * Continuous location update flow.
     */
    fun getLocationUpdates(intervalMs: Long): Flow<GeoLocation>
}
