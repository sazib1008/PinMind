package com.example.pinmind.core.location

/**
 * Represents the 3-state location permission status on Android 10+.
 *
 * - [Denied]: Neither foreground nor background location is granted.
 * - [ForegroundOnly]: Only ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted (While using the app).
 * - [GrantedAllTime]: Both foreground and ACCESS_BACKGROUND_LOCATION are granted (Allow all the time).
 */
enum class LocationPermissionState {
    Denied,
    ForegroundOnly,
    GrantedAllTime
}
