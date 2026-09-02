package com.example.pinmind.domain.usecase

import com.example.pinmind.core.location.GeocoderHelper
import javax.inject.Inject

/**
 * Use case to reverse-geocode coordinates into a human-readable location name and formatted address.
 */
class ReverseGeocodeUseCase @Inject constructor(
    private val geocoderHelper: GeocoderHelper
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double
    ): Result<Pair<String, String?>> {
        return try {
            val result = geocoderHelper.getAddressFromCoordinates(latitude, longitude)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
