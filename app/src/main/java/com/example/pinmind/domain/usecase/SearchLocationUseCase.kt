package com.example.pinmind.domain.usecase

import com.example.pinmind.core.location.NominatimSearchHelper
import com.example.pinmind.domain.model.SearchLocationResult
import javax.inject.Inject

/**
 * Use case to search locations by text query using OpenStreetMap Nominatim API.
 */
class SearchLocationUseCase @Inject constructor(
    private val nominatimSearchHelper: NominatimSearchHelper
) {
    suspend operator fun invoke(query: String): List<SearchLocationResult> {
        return nominatimSearchHelper.searchLocations(query)
    }
}
