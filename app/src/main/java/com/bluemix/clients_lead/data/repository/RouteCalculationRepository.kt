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

data class RouteResult(
    val distanceKm: Double,
    val durationMinutes: Int,
    val routePolyline: List<LatLng>? = null
)

class RouteCalculationRepository(
    private val httpClient: HttpClient
) {
    companion object {
        private const val OSRM_BASE = "https://router.project-osrm.org"
        private const val OVERPASS_API = "https://overpass-api.de/api/interpreter"
    }

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

            TransportMode.BUS -> calculateRoadDistance(start, end, "car")

            TransportMode.TRAIN,
            TransportMode.METRO -> calculateRailDistance(start, end)

            TransportMode.FLIGHT -> calculateFlightDistance(start, end)
        }
    }

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
            TransportMode.METRO -> calculateTrainRouteWithGoogle(start, end)

            TransportMode.FLIGHT -> calculateFlightRouteWithGeometry(start, end)
        }
    }

    // ✅ FIXED: Proper validation with railway station checking
    suspend fun validateTransportMode(
        start: LocationPlace,
        end: LocationPlace,
        transportMode: TransportMode
    ): Pair<Boolean, String?> {
        return when (transportMode) {
            TransportMode.TRAIN, TransportMode.METRO -> {
                val distance = calculateStraightLineDistance(start, end)

                // Basic distance checks
                when {
                    distance < 5.0 -> {
                        return false to "Train not recommended for distances under 5 KM. Try Bus or Bike instead."
                    }
                    distance > 500.0 -> {
                        return false to "Distance too long for train calculation. Consider Flight mode."
                    }
                }

                // Check for nearby railway stations
                try {
                    Timber.d("🚂 Checking for railway stations near locations...")
                    val hasStations = checkNearbyTrainStations(start, end)

                    if (!hasStations) {
                        false to "No railway stations found within 3 KM of start or end location. Try Bus or Rickshaw instead."
                    } else {
                        Timber.d("✅ Railway stations found, train mode available")
                        true to null
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Station check failed, allowing train mode anyway")
                    // If check fails, allow train mode (graceful degradation)
                    true to null
                }
            }

            TransportMode.FLIGHT -> {
                val distance = calculateStraightLineDistance(start, end)
                if (distance < 200.0) {
                    false to "Flight mode is only available for distances over 200 KM."
                } else {
                    true to null
                }
            }

            else -> true to null // Road modes always available
        }
    }

    // ✅ FIXED: Check for railway stations using Overpass API
    private suspend fun checkNearbyTrainStations(
        start: LocationPlace,
        end: LocationPlace
    ): Boolean {
        val startHasStation = searchNearbyStations(start.latitude, start.longitude)
        val endHasStation = searchNearbyStations(end.latitude, end.longitude)

        Timber.d("🚉 Station check: start=$startHasStation, end=$endHasStation")
        return startHasStation && endHasStation
    }

    // ✅ FIXED: Use Overpass API to find actual railway stations
    private suspend fun searchNearbyStations(lat: Double, lon: Double): Boolean {
        return try {
            val radius = 3000 // 3km search radius

            // Overpass QL query to find railway stations
            val query = """
                [out:json][timeout:10];
                (
                  node["railway"="station"](around:$radius,$lat,$lon);
                  node["railway"="halt"](around:$radius,$lat,$lon);
                  node["public_transport"="station"]["train"="yes"](around:$radius,$lat,$lon);
                );
                out body;
            """.trimIndent()

            Timber.d("🔍 Searching for stations near ($lat, $lon)...")

            val response: OverpassResponse = httpClient.get(OVERPASS_API) {
                parameter("data", query)
            }.body()

            val stationCount = response.elements.size
            Timber.d("🚉 Found $stationCount railway stations within 3km")

            if (stationCount > 0) {
                // Log station names for debugging
                response.elements.take(3).forEach { station ->
                    val name = station.tags?.get("name") ?: "Unknown"
                    Timber.d("  - $name")
                }
            }

            stationCount > 0

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to search stations via Overpass API")
            // Return true to allow train mode if API fails
            true
        }
    }

    private suspend fun calculateRoadDistance(
        start: LocationPlace,
        end: LocationPlace,
        profile: String
    ): Double {
        return try {
            val url = "$OSRM_BASE/route/v1/$profile/${start.longitude},${start.latitude};${end.longitude},${end.latitude}"

            val response: OsrmResponse = httpClient.get(url) {
                parameter("overview", "false")
                parameter("steps", "false")
            }.body()

            val distanceMeters = response.routes.firstOrNull()?.distance ?: 0.0
            (distanceMeters / 1000.0).round(2)

        } catch (e: Exception) {
            Timber.e(e, "❌ OSRM routing failed, using straight-line")
            calculateStraightLineDistance(start, end)
        }
    }

    private suspend fun calculateRoadRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace,
        profile: String
    ): RouteResult {
        return try {
            val url = "$OSRM_BASE/route/v1/$profile/${start.longitude},${start.latitude};${end.longitude},${end.latitude}"

            val response: OsrmResponse = httpClient.get(url) {
                parameter("overview", "full")
                parameter("geometries", "geojson")
            }.body()

            val route = response.routes.firstOrNull()
            val distanceKm = (route?.distance ?: 0.0) / 1000.0
            val durationMinutes = ((route?.duration ?: 0.0) / 60.0).toInt()

            val polyline = route?.geometry?.coordinates?.map { coord ->
                LatLng(coord[1], coord[0])
            } ?: emptyList()

            RouteResult(
                distanceKm = distanceKm.round(2),
                durationMinutes = durationMinutes,
                routePolyline = polyline
            )

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get route geometry")
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

    private suspend fun calculateRailDistance(
        start: LocationPlace,
        end: LocationPlace
    ): Double {
        val straightLine = calculateStraightLineDistance(start, end)
        return (straightLine * 1.2).round(2)
    }

    // ✅ NOW ACTUALLY USED: Google Directions API for train routes
    private suspend fun calculateTrainRouteWithGoogle(
        start: LocationPlace,
        end: LocationPlace
    ): RouteResult {
        return try {
            val apiKey = "AIzaSyCwGkrq4Onpvj9Yu5His9row-fIg5v6N0I"

            Timber.d("🚂 Fetching train route from Google Directions API...")

            val response: GoogleDirectionsResponse = httpClient.get(
                "https://maps.googleapis.com/maps/api/directions/json"
            ) {
                parameter("origin", "${start.latitude},${start.longitude}")
                parameter("destination", "${end.latitude},${end.longitude}")
                parameter("mode", "transit")
                parameter("transit_mode", "rail")
                parameter("key", apiKey)
            }.body()

            if (response.status != "OK") {
                Timber.w("⚠️ Google API status: ${response.status}, falling back to approximation")
                return calculateRailRouteWithGeometry(start, end)
            }

            val route = response.routes.firstOrNull()
            val leg = route?.legs?.firstOrNull()

            if (leg != null && route.overviewPolyline.points.isNotEmpty()) {
                val polyline = decodePolyline(route.overviewPolyline.points)

                Timber.d("✅ Google train route: ${leg.distance.value / 1000.0} km, ${leg.duration.value / 60} min")

                RouteResult(
                    distanceKm = (leg.distance.value / 1000.0).round(2),
                    durationMinutes = (leg.duration.value / 60),
                    routePolyline = polyline
                )
            } else {
                Timber.w("⚠️ No route found in Google response, falling back")
                calculateRailRouteWithGeometry(start, end)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get train route from Google")
            calculateRailRouteWithGeometry(start, end)
        }
    }

    private suspend fun calculateRailRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace
    ): RouteResult {
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

    private suspend fun calculateFlightDistance(
        start: LocationPlace,
        end: LocationPlace
    ): Double {
        return calculateStraightLineDistance(start, end)
    }

    private suspend fun calculateFlightRouteWithGeometry(
        start: LocationPlace,
        end: LocationPlace
    ): RouteResult {
        return RouteResult(
            distanceKm = calculateStraightLineDistance(start, end),
            durationMinutes = 0,
            routePolyline = listOf(
                LatLng(start.latitude, start.longitude),
                LatLng(end.latitude, end.longitude)
            )
        )
    }

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
    val distance: Double,
    val duration: Double,
    val geometry: OsrmGeometry? = null
)

@Serializable
data class OsrmGeometry(
    val coordinates: List<List<Double>>,
    val type: String = "LineString"
)

// Google Directions API DTOs
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

// ✅ Overpass API DTOs for railway station search
@Serializable
data class OverpassResponse(
    val elements: List<OverpassElement>
)

@Serializable
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String>? = null
)