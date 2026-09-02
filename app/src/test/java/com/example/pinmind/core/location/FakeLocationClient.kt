package com.example.pinmind.core.location

import com.example.pinmind.domain.model.GeoLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake in-memory implementation of [LocationClient] for unit tests.
 */
class FakeLocationClient : LocationClient {

    var locationToReturn: GeoLocation? = null
    private val locationFlow = MutableStateFlow<GeoLocation?>(null)

    override suspend fun getCurrentLocation(): GeoLocation? {
        return locationToReturn
    }

    @Suppress("UNCHECKED_CAST")
    override fun getLocationUpdates(intervalMs: Long): Flow<GeoLocation> {
        return locationFlow as Flow<GeoLocation>
    }

    fun emitLocation(location: GeoLocation) {
        locationFlow.value = location
    }
}
