// data/repository/RouteCalculationRepository.kt
package com.bluemix.clients_lead.data.repository

import android.location.Location
import com.bluemix.clients_lead.domain.model.LocationPlace
import com.bluemix.clients_lead.domain.model.TransportMode
import com.google.android.gms.maps.model.LatLng
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * Result containing route information
 */
data class RouteResult(
    val distanceKm: Double,
    val durationMinutes: Int,
    val routePolyline: List<LatLng>? = null
)

/**
 * FREE Routing APIs:
 * 1. OSRM (OpenStreetMap) - Road routing - NO API KEY
 * 2. Straight-line calculations for rail/flight
 */
class RouteCalculationRepository(
    private val httpClient: HttpClient
) {
    companion object {
        // OSRM for road-based transport (car, bus, bike)
        private const val OSRM_BASE = "https://router.project-osrm.org"
    }

    /**
     * Calculate route distance based on transport mode
     */
    suspend fun calculateRouteDistance(
        start: LocationPlace,
        end: LocationPlace,
        transportMode: TransportMode
    ): Double {
        return when (transportMode) {
            TransportMode.CAR,
            TransportMode.TAXI,
            TransportMode.RICKSHAW -> calculateRoadDistance(start, end, "car")

            TransportMode.BIKE -> calculateRoadDistance(start, end, "bike")

            TransportMode.BUS -> calculateRoadDistance(start, end, "car") // Buses follow roads

            TransportMode.TRAIN,
            TransportMode.METRO -> calculateRailDistance(start, end)

            TransportMode.FLIGHT -> calculateFlightDistance(start, end)
        }
    }

    /**
     * Calculate route with geometry for visualization
     */
    suspend fun calculateRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace,
        transportMode: TransportMode
    ): RouteResult {
        return when (transportMode) {
            TransportMode.CAR,
            TransportMode.TAXI,
            TransportMode.RICKSHAW -> calculateRoadRouteWithGeometry(start, end, "car")

            TransportMode.BIKE -> calculateRoadRouteWithGeometry(start, end, "bike")

            TransportMode.BUS -> calculateRoadRouteWithGeometry(start, end, "car")

            TransportMode.TRAIN,
            TransportMode.METRO -> calculateRailRouteWithGeometry(start, end)

            TransportMode.FLIGHT -> calculateFlightRouteWithGeometry(start, end)
        }
    }

    /**
     * Get road-based distance using OSRM (FREE, no API key)
     */
    private suspend fun calculateRoadDistance(
        start: LocationPlace,
        end: LocationPlace,
        profile: String // "car" or "bike"
    ): Double {
        return try {
            val url = "$OSRM_BASE/route/v1/$profile/${start.longitude},${start.latitude};${end.longitude},${end.latitude}"

            Timber.d("🛣️ Calculating road distance via OSRM: $profile")

            val response: OsrmResponse = httpClient.get(url) {
                parameter("overview", "false")
                parameter("steps", "false")
            }.body()

            val distanceMeters = response.routes.firstOrNull()?.distance ?: 0.0
            val distanceKm = distanceMeters / 1000.0

            Timber.d("✅ Road distance: ${String.format("%.2f", distanceKm)} km")
            distanceKm.round(2)

        } catch (e: Exception) {
            Timber.e(e, "❌ OSRM routing failed, falling back to straight-line")
            calculateStraightLineDistance(start, end)
        }
    }

    /**
     * Get road route with geometry from OSRM
     */
    private suspend fun calculateRoadRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace,
        profile: String
    ): RouteResult {
        return try {
            val url = "$OSRM_BASE/route/v1/$profile/${start.longitude},${start.latitude};${end.longitude},${end.latitude}"

            Timber.d("🛣️ Calculating road route with geometry: $profile")

            val response: OsrmResponse = httpClient.get(url) {
                parameter("overview", "full") // Get full geometry
                parameter("geometries", "geojson") // GeoJSON format
            }.body()

            val route = response.routes.firstOrNull()
            val distanceKm = (route?.distance ?: 0.0) / 1000.0
            val durationMinutes = ((route?.duration ?: 0.0) / 60.0).toInt()

            // Convert GeoJSON coordinates to LatLng
            val polyline = route?.geometry?.coordinates?.map { coord ->
                LatLng(coord[1], coord[0]) // GeoJSON is [lon, lat]
            } ?: emptyList()

            Timber.d("✅ Road route: ${String.format("%.2f", distanceKm)} km, $durationMinutes min, ${polyline.size} points")

            RouteResult(
                distanceKm = distanceKm.round(2),
                durationMinutes = durationMinutes,
                routePolyline = polyline
            )

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get route geometry")
            // Fallback: straight line
            RouteResult(
                distanceKm = calculateStraightLineDistance(start, end),
                durationMinutes = 0,
                routePolyline = listOf(
                    LatLng(start.latitude, start.longitude),
                    LatLng(end.latitude, end.longitude)
                )
            )
        }
    }

    /**
     * Rail distance - approximate using straight-line + 20% detour factor
     */
    private suspend fun calculateRailDistance(
        start: LocationPlace,
        end: LocationPlace
    ): Double {
        Timber.d("🚂 Calculating rail distance (straight-line + 20%)")
        val straightLine = calculateStraightLineDistance(start, end)
        val railDistance = straightLine * 1.2 // Rail routes typically 20% longer
        return railDistance.round(2)
    }

    /**
     * Rail route with geometry - approximate with straight line
     */
    private suspend fun calculateRailRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace
    ): RouteResult {
        Timber.d("🚂 Calculating rail route (straight-line approximation)")
        val straightLine = calculateStraightLineDistance(start, end)
        return RouteResult(
            distanceKm = (straightLine * 1.2).round(2),
            durationMinutes = 0,
            routePolyline = listOf(
                LatLng(start.latitude, start.longitude),
                LatLng(end.latitude, end.longitude)
            )
        )
    }

    // Add to RouteCalculationRepository.kt

    /**
     * Check if transport mode is available/feasible for the given route
     */
    suspend fun validateTransportMode(
        start: LocationPlace,
        end: LocationPlace,
        transportMode: TransportMode
    ): Pair<Boolean, String?> {
        return when (transportMode) {
            TransportMode.TRAIN, TransportMode.METRO -> {
                // Check if there are nearby train stations
                val nearbyStations = checkNearbyTrainStations(start, end)
                if (!nearbyStations) {
                    false to "No train stations found nearby. Consider using Bus or Car instead."
                } else {
                    true to null
                }
            }
            TransportMode.FLIGHT -> {
                // Only valid for distances > 200km
                val distance = calculateStraightLineDistance(start, end)
                if (distance < 200.0) {
                    false to "Flight mode is only available for distances over 200 KM."
                } else {
                    true to null
                }
            }
            else -> true to null // Road-based modes are always valid
        }
    }

    // Add to RouteCalculationRepository.kt

    /**
     * Get train route using Google Directions API (REQUIRES API KEY)
     */
    private suspend fun calculateTrainRouteWithGoogle(
        start: LocationPlace,
        end: LocationPlace
    ): RouteResult {
        return try {
            val apiKey = "AIzaSyCwGkrq4Onpvj9Yu5His9row-fIg5v6N0I" // Add to BuildConfig

            val response: GoogleDirectionsResponse = httpClient.get(
                "https://maps.googleapis.com/maps/api/directions/json"
            ) {
                parameter("origin", "${start.latitude},${start.longitude}")
                parameter("destination", "${end.latitude},${end.longitude}")
                parameter("mode", "transit")
                parameter("transit_mode", "rail")
                parameter("key", apiKey)
            }.body()

            val route = response.routes.firstOrNull()
            val leg = route?.legs?.firstOrNull()

            if (leg != null) {
                // Decode polyline points
                val polyline = decodePolyline(route.overviewPolyline.points)

                RouteResult(
                    distanceKm = (leg.distance.value / 1000.0).round(2),
                    durationMinutes = (leg.duration.value / 60),
                    routePolyline = polyline
                )
            } else {
                // Fallback to approximation
                calculateRailRouteWithGeometry(start, end)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get train route from Google")
            calculateRailRouteWithGeometry(start, end)
        }
    }

    /**
     * Decode Google's polyline encoding
     */
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    /**
     * Check if there are train stations within 2km of start or end location
     */
    private suspend fun checkNearbyTrainStations(
        start: LocationPlace,
        end: LocationPlace
    ): Boolean {
        // Search for train stations near start and end
        val startStations = searchNearbyStations(start.latitude, start.longitude)
        val endStations = searchNearbyStations(end.latitude, end.longitude)

        return startStations.isNotEmpty() && endStations.isNotEmpty()
    }

    private suspend fun searchNearbyStations(lat: Double, lon: Double): List<String> {
        return try {
            // Search for railway stations within 2km
            val response: OsrmResponse = httpClient.get(
                "$OSRM_BASE/nearest/v1/driving/$lon,$lat"
            ) {
                parameter("number", 5)
            }.body()

            // Filter for train stations (simplified - you'd want better filtering)
            response.routes.map { it.toString() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to search nearby stations")
            emptyList()
        }
    }

    /**
     * Flight distance - straight-line (great circle)
     */
    private suspend fun calculateFlightDistance(
        start: LocationPlace,
        end: LocationPlace
    ): Double {
        Timber.d("✈️ Calculating flight distance (great circle)")
        return calculateStraightLineDistance(start, end)
    }

    /**
     * Flight route with geometry - straight line
     */
    private suspend fun calculateFlightRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace
    ): RouteResult {
        Timber.d("✈️ Calculating flight route (straight-line)")
        return RouteResult(
            distanceKm = calculateStraightLineDistance(start, end),
            durationMinutes = 0,
            routePolyline = listOf(
                LatLng(start.latitude, start.longitude),
                LatLng(end.latitude, end.longitude)
            )
        )
    }

    /**
     * Fallback: Haversine straight-line distance
     */
    private fun calculateStraightLineDistance(
        start: LocationPlace,
        end: LocationPlace
    ): Double {
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

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}

// OSRM Response DTOs
@Serializable
data class OsrmResponse(
    val routes: List<OsrmRoute>,
    val code: String
)

@Serializable
data class OsrmRoute(
    val distance: Double, // meters
    val duration: Double, // seconds
    val geometry: OsrmGeometry? = null
)

@Serializable
data class OsrmGeometry(
    val coordinates: List<List<Double>>, // [[lon, lat], [lon, lat], ...]
    val type: String = "LineString"
)


// Google Directions API response models
@Serializable
data class GoogleDirectionsResponse(
    val routes: List<GoogleRoute>,
    val status: String
)

@Serializable
data class GoogleRoute(
    val legs: List<GoogleLeg>,
    @SerialName("overview_polyline")
    val overviewPolyline: GooglePolyline
)

@Serializable
data class GoogleLeg(
    val distance: GoogleDistance,
    val duration: GoogleDuration
)

@Serializable
data class GoogleDistance(val value: Int, val text: String)

@Serializable
data class GoogleDuration(val value: Int, val text: String)

@Serializable
data class GooglePolyline(val points: String)
