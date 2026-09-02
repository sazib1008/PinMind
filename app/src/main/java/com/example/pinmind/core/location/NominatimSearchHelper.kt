package com.example.pinmind.core.location

import android.content.Context
import com.example.pinmind.domain.model.SearchLocationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper to query OpenStreetMap's Nominatim Search API for location suggestions.
 * Follows OpenStreetMap Nominatim usage policy by providing an explicit User-Agent.
 */
@Singleton
open class NominatimSearchHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    open suspend fun searchLocations(query: String): List<SearchLocationResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val urlString = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&addressdetails=1&limit=5"

        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "PinMindApp/1.0 (${context.packageName})"
                )
                connectTimeout = 6000
                readTimeout = 6000
            }

            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(responseText)
                    val results = mutableListOf<SearchLocationResult>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val lat = item.getString("lat").toDoubleOrNull() ?: continue
                        val lon = item.getString("lon").toDoubleOrNull() ?: continue
                        val displayName = item.getString("display_name")
                        val name = item.optString("name").ifBlank {
                            displayName.split(",").firstOrNull()?.trim() ?: displayName
                        }
                        results.add(
                            SearchLocationResult(
                                displayName = displayName,
                                shortName = name,
                                latitude = lat,
                                longitude = lon
                            )
                        )
                    }
                    results
                } else {
                    emptyList()
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
