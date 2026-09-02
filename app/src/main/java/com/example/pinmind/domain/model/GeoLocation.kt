package com.example.pinmind.domain.model

/**
 * Pure domain model representing a geographic location with an attached geofence radius.
 *
 * @property latitude Target latitude coordinate.
 * @property longitude Target longitude coordinate.
 * @property radiusMeters Geofence trigger radius in meters (default 100m).
 * @property locationName Human-readable location name or label (e.g., "Grocery Store", "Office").
 * @property address Optional street or formatted address.
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val locationName: String = "",
    val address: String? = null
)
