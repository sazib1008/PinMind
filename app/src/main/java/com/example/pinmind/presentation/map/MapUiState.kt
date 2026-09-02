package com.example.pinmind.presentation.map

import com.example.pinmind.core.location.LocationPermissionState
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.model.SearchLocationResult

/**
 * UI State for the Map Picker screen.
 */
data class MapUiState(
    val selectedLocation: GeoLocation? = null,
    val currentDeviceLocation: GeoLocation? = null,
    val radiusMeters: Float = 100f,
    val searchQuery: String = "",
    val searchResults: List<SearchLocationResult> = emptyList(),
    val isSearching: Boolean = false,
    val isResolvingAddress: Boolean = false,
    val permissionState: LocationPermissionState = LocationPermissionState.Denied,
    val showPermissionFlow: Boolean = false,
    val errorMessage: String? = null
)
