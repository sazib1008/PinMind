package com.example.pinmind.domain.model

/**
 * Geofence transition trigger types.
 * Per platform constraints, default is ENTER_OR_DWELL to avoid false triggers.
 */
enum class GeofenceTransitionType {
    ENTER,
    EXIT,
    DWELL,
    ENTER_OR_DWELL
}
