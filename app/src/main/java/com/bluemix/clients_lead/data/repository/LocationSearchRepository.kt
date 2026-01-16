// data/repository/LocationSearchRepository.kt
package com.bluemix.clients_lead.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.bluemix.clients_lead.data.remote.NominatimApiService
import com.bluemix.clients_lead.domain.model.LocationPlace
import com.bluemix.clients_lead.data.repository.RouteResult
import com.bluemix.clients_lead.domain.model.TransportMode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Repository for location search and geocoding
 * Uses FREE Nominatim API (no API key needed)
 */
class LocationSearchRepository(
    private val context: Context,
    private val nominatimApi: NominatimApiService,
    private val routeCalculator: RouteCalculationRepository // ✅ NEW
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Get current location using device GPS
     * Returns address via reverse geocoding
     */
    suspend fun getCurrentLocation(): LocationPlace? {
        if (!hasLocationPermission()) {
            Timber.w("❌ Location permission not granted")
            return null
        }

        return try {
            // Get GPS coordinates
            val location: Location? = fusedLocationClient.lastLocation.await()

            if (location == null) {
                Timber.w("⚠️ Location is null")
                return null
            }

            Timber.d("📍 Got location: ${location.latitude}, ${location.longitude}")

            // Reverse geocode to get address
            delay(1000) // Rate limiting: 1 request/second
            val result = nominatimApi.reverseGeocode(
                lat = location.latitude,
                lon = location.longitude
            )

            result?.let {
                LocationPlace(
                    displayName = it.displayName,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            } ?: LocationPlace(
                displayName = "Current Location",
                latitude = location.latitude,
                longitude = location.longitude
            )
        } catch (e: SecurityException) {
            Timber.e(e, "❌ SecurityException getting location")
            null
        } catch (e: Exception) {
            Timber.e(e, "❌ Error getting location")
            null
        }
    }

    /**
     * Search for places by query
     * Uses FREE Nominatim API
     */
    suspend fun searchPlaces(query: String): List<LocationPlace> {
        if (query.length < 3) {
            return emptyList()
        }

        return try {
            // Rate limiting: 1 request/second
            delay(1000)

            val results = nominatimApi.searchPlaces(
                query = query,
                limit = 5
            )

            results.map { result ->
                LocationPlace(
                    displayName = result.displayName,
                    latitude = result.lat.toDouble(),
                    longitude = result.lon.toDouble()
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Search failed")
            emptyList()
        }
    }
    suspend fun validateTransportMode(
        start: LocationPlace,
        end: LocationPlace,
        transportMode: TransportMode
    ): Pair<Boolean, String?> {
        return routeCalculator.validateTransportMode(start, end, transportMode)
    }

    /**
     * ✅ NEW: Calculate route distance based on transport mode
     */
    suspend fun calculateRouteDistanceKm(
        start: LocationPlace,
        end: LocationPlace,
        transportMode: TransportMode
    ): Double {
        return routeCalculator.calculateRouteDistance(start, end, transportMode)
    }

    /**
     * ✅ NEW: Get route with geometry for visualization
     */
    suspend fun calculateRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace,
        transportMode: TransportMode
    ): RouteResult {
        return routeCalculator.calculateRouteWithGeometry(start, end, transportMode)
    }

    /**
     * Calculate straight-line distance between two points in kilometers
     * Kept for backwards compatibility
     */
    fun calculateDistanceKm(start: LocationPlace, end: LocationPlace): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude,
            results
        )
        return (results[0] / 1000.0).round(2)
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}