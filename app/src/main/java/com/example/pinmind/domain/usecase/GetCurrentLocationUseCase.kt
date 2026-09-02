package com.example.pinmind.domain.usecase

import com.example.pinmind.core.location.LocationClient
import com.example.pinmind.domain.model.GeoLocation
import javax.inject.Inject

/**
 * Use case to retrieve the user's current device location.
 */
class GetCurrentLocationUseCase @Inject constructor(
    private val locationClient: LocationClient
) {
    /**
     * Obtains the current location snapshot.
     *
     * @return [Result] containing [GeoLocation] or an error if unavailable.
     */
    suspend operator fun invoke(): Result<GeoLocation> {
        return try {
            val location = locationClient.getCurrentLocation()
            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(IllegalStateException("Location unavailable. Please ensure GPS and permissions are enabled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
