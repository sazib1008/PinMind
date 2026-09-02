package com.example.pinmind.core.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Helper to perform reverse-geocoding into human-readable place names and addresses.
 */
open class GeocoderHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Resolves a latitude/longitude pair into a friendly location name and formatted address.
     */
    open suspend fun getAddressFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Pair<String, String?> = withContext(Dispatchers.IO) {

        if (!Geocoder.isPresent()) {
            return@withContext "Custom Location" to "(${String.format(Locale.US, "%.4f", latitude)}, ${String.format(Locale.US, "%.4f", longitude)})"
        }

        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                val address = addresses.firstOrNull()
                                if (address != null) {
                                    val name = address.featureName
                                        ?: address.thoroughfare
                                        ?: address.locality
                                        ?: "Selected Location"
                                    val fullAddress = (0..address.maxAddressLineIndex)
                                        .mapNotNull { address.getAddressLine(it) }
                                        .joinToString(", ")
                                    continuation.resume(name to fullAddress)
                                } else {
                                    continuation.resume("Selected Location" to null)
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                continuation.resume("Selected Location" to null)
                            }
                        }
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    val name = address.featureName
                        ?: address.thoroughfare
                        ?: address.locality
                        ?: "Selected Location"
                    val fullAddress = (0..address.maxAddressLineIndex)
                        .mapNotNull { address.getAddressLine(it) }
                        .joinToString(", ")
                    name to fullAddress
                } else {
                    "Selected Location" to null
                }
            }
        } catch (e: Exception) {
            "Selected Location" to "(${String.format(Locale.US, "%.4f", latitude)}, ${String.format(Locale.US, "%.4f", longitude)})"
        }
    }
}
