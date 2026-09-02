package com.example.pinmind.domain.model

/**
 * Domain model representing a location search suggestion from Nominatim.
 *
 * @property displayName Full formatted address returned by Nominatim.
 * @property shortName Brief name (e.g. landmark, venue, or street name).
 * @property latitude Geographic latitude.
 * @property longitude Geographic longitude.
 */
data class SearchLocationResult(
    val displayName: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double
)
