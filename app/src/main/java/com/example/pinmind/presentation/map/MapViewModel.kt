package com.example.pinmind.presentation.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pinmind.core.location.LocationPermissionState
import com.example.pinmind.domain.model.GeoLocation
import com.example.pinmind.domain.usecase.GetCurrentLocationUseCase
import com.example.pinmind.domain.usecase.ReverseGeocodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel handling map interaction, reverse-geocoding, radius updates, and location selection.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val reverseGeocodeUseCase: ReverseGeocodeUseCase
) : ViewModel() {

    private val initialLat: Double? = savedStateHandle.get<String>("lat")?.toDoubleOrNull()
    private val initialLng: Double? = savedStateHandle.get<String>("lng")?.toDoubleOrNull()
    private val initialRadius: Float = savedStateHandle.get<String>("radius")?.toFloatOrNull() ?: 100f

    private val _uiState = MutableStateFlow(
        MapUiState(
            radiusMeters = initialRadius,
            selectedLocation = if (initialLat != null && initialLng != null) {
                GeoLocation(
                    latitude = initialLat,
                    longitude = initialLng,
                    radiusMeters = initialRadius
                )
            } else {
                null
            }
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var geocodeJob: Job? = null

    init {
        if (initialLat != null && initialLng != null) {
            resolveLocationDetails(initialLat, initialLng, initialRadius)
        } else {
            fetchDeviceLocation()
        }
    }

    fun onPermissionStateUpdated(state: LocationPermissionState) {
        _uiState.update { it.copy(permissionState = state, showPermissionFlow = false) }
        if (state != LocationPermissionState.Denied && _uiState.value.selectedLocation == null) {
            fetchDeviceLocation()
        }
    }

    fun requestPermissionFlow() {
        _uiState.update { it.copy(showPermissionFlow = true) }
    }

    fun dismissPermissionFlow() {
        _uiState.update { it.copy(showPermissionFlow = false) }
    }

    fun fetchDeviceLocation() {
        viewModelScope.launch {
            val result = getCurrentLocationUseCase()
            if (result.isSuccess) {
                val loc = result.getOrNull()
                if (loc != null) {
                    _uiState.update {
                        it.copy(
                            currentDeviceLocation = loc,
                            selectedLocation = if (it.selectedLocation == null) loc.copy(radiusMeters = it.radiusMeters) else it.selectedLocation
                        )
                    }
                    if (_uiState.value.selectedLocation?.locationName.isNullOrBlank()) {
                        resolveLocationDetails(loc.latitude, loc.longitude, _uiState.value.radiusMeters)
                    }
                }
            } else {
                // If device location fails or no permission, fallback to default coordinate (e.g. San Francisco or center)
                if (_uiState.value.selectedLocation == null) {
                    val defaultLoc = GeoLocation(
                        latitude = 37.7749,
                        longitude = -122.4194,
                        radiusMeters = _uiState.value.radiusMeters,
                        locationName = "Selected Location"
                    )
                    _uiState.update { it.copy(selectedLocation = defaultLoc) }
                    resolveLocationDetails(defaultLoc.latitude, defaultLoc.longitude, defaultLoc.radiusMeters)
                }
            }
        }
    }

    fun onMapTapped(latitude: Double, longitude: Double) {
        val currentRadius = _uiState.value.radiusMeters
        _uiState.update {
            it.copy(
                selectedLocation = GeoLocation(
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = currentRadius
                )
            )
        }
        resolveLocationDetails(latitude, longitude, currentRadius)
    }

    fun onRadiusChanged(newRadius: Float) {
        _uiState.update { current ->
            current.copy(
                radiusMeters = newRadius,
                selectedLocation = current.selectedLocation?.copy(radiusMeters = newRadius)
            )
        }
    }

    private fun resolveLocationDetails(latitude: Double, longitude: Double, radius: Float) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            _uiState.update { it.copy(isResolvingAddress = true) }
            delay(300) // Debounce rapid map changes
            val result = reverseGeocodeUseCase(latitude, longitude)
            if (result.isSuccess) {
                val (name, address) = result.getOrNull() ?: ("Selected Location" to null)
                _uiState.update { current ->
                    current.copy(
                        isResolvingAddress = false,
                        selectedLocation = GeoLocation(
                            latitude = latitude,
                            longitude = longitude,
                            radiusMeters = radius,
                            locationName = name,
                            address = address
                        )
                    )
                }
            } else {
                _uiState.update { it.copy(isResolvingAddress = false) }
            }
        }
    }
}
