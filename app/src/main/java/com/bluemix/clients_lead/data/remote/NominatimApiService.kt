package com.bluemix.clients_lead.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * FREE Nominatim API service using OpenStreetMap
 * NO API KEY REQUIRED - Rate limit: 1 request/second
 */
class NominatimApiService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://nominatim.openstreetmap.org"
        private const val USER_AGENT = "GeoTrackApp/1.0" // REQUIRED by Nominatim
    }

    /**
     * Search for places by query
     * @param query Search term (e.g., "Delhi Railway Station")
     * @param limit Max results (default: 5)
     * @return List of matching places
     */
    suspend fun searchPlaces(
        query: String,
        limit: Int = 5
    ): List<NominatimSearchResult> {
        Timber.d("🔍 Searching Nominatim: $query")

        return try {
            val results: List<NominatimSearchResult> = httpClient.get("$BASE_URL/search") {
                header("User-Agent", USER_AGENT)
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", limit)
                parameter("countrycodes", "in") // India only
                parameter("addressdetails", 1)
            }.body()

            Timber.d("✅ Found ${results.size} results")
            results
        } catch (e: Exception) {
            Timber.e(e, "❌ Nominatim search failed")
            emptyList()
        }
    }

    /**
     * Reverse geocode: Convert coordinates to address
     * @param lat Latitude
     * @param lon Longitude
     * @return Place information
     */
    suspend fun reverseGeocode(
        lat: Double,
        lon: Double
    ): NominatimSearchResult? {
        Timber.d("📍 Reverse geocoding: $lat, $lon")

        return try {
            httpClient.get("$BASE_URL/reverse") {
                header("User-Agent", USER_AGENT)
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("format", "json")
            }.body()
        } catch (e: Exception) {
            Timber.e(e, "❌ Reverse geocode failed")
            null
        }
    }
}

/**
 * Nominatim search result
 */
@Serializable
data class NominatimSearchResult(
    @SerialName("display_name")
    val displayName: String,

    @SerialName("lat")
    val lat: String,

    @SerialName("lon")
    val lon: String,

    @SerialName("place_id")
    val placeId: Long,

    @SerialName("type")
    val type: String? = null,

    @SerialName("importance")
    val importance: Double? = null
)