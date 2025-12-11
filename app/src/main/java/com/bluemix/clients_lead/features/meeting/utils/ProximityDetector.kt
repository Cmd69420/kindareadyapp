package com.bluemix.clients_lead.features.meeting.utils

import android.location.Location
import com.bluemix.clients_lead.domain.model.Client
import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility class for detecting proximity to clients
 */
object ProximityDetector {

    // Default proximity radius in meters
    const val DEFAULT_PROXIMITY_RADIUS_METERS = 100.0

    /**
     * Check if current location is within proximity of a client
     */
    fun isWithinProximity(
        currentLocation: LatLng,
        client: Client,
        radiusMeters: Double = DEFAULT_PROXIMITY_RADIUS_METERS
    ): Boolean {
        if (client.latitude == null || client.longitude == null) {
            return false
        }

        val clientLocation = LatLng(client.latitude, client.longitude)
        val distance = calculateDistance(currentLocation, clientLocation)

        return distance <= radiusMeters
    }

    /**
     * Find all clients within proximity of current location
     */
    fun findClientsInProximity(
        currentLocation: LatLng,
        clients: List<Client>,
        radiusMeters: Double = DEFAULT_PROXIMITY_RADIUS_METERS
    ): List<Client> {
        return clients.filter { client ->
            isWithinProximity(currentLocation, client, radiusMeters)
        }
    }

    /**
     * Find the nearest client to current location
     */
    fun findNearestClient(
        currentLocation: LatLng,
        clients: List<Client>
    ): Pair<Client, Double>? {
        return clients
            .mapNotNull { client ->
                if (client.latitude != null && client.longitude != null) {
                    val distance = calculateDistance(
                        currentLocation,
                        LatLng(client.latitude, client.longitude)
                    )
                    client to distance
                } else {
                    null
                }
            }
            .minByOrNull { it.second }
    }

    /**
     * Calculate distance between two LatLng points using Haversine formula
     * Returns distance in meters
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadiusMeters = 6371000.0 // Earth's radius in meters

        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLng / 2) * sin(deltaLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    /**
     * Format distance for display
     */
    fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()} m"
            else -> String.format("%.1f km", distanceMeters / 1000)
        }
    }

    /**
     * Check if user just entered proximity (to prevent repeated triggers)
     */
    data class ProximityState(
        val clientId: String,
        val wasInProximity: Boolean,
        val lastCheckTime: Long
    )

    private val proximityStates = mutableMapOf<String, ProximityState>()

    /**
     * Detect proximity entry (returns true only when first entering proximity)
     */
    fun detectProximityEntry(
        currentLocation: LatLng,
        client: Client,
        radiusMeters: Double = DEFAULT_PROXIMITY_RADIUS_METERS,
        cooldownMillis: Long = 300000 // 5 minutes cooldown
    ): Boolean {
        val isCurrentlyInProximity = isWithinProximity(currentLocation, client, radiusMeters)
        val now = System.currentTimeMillis()

        val previousState = proximityStates[client.id]

        // Check if this is a new entry (wasn't in proximity before, but is now)
        val isNewEntry = if (previousState != null) {
            !previousState.wasInProximity &&
                    isCurrentlyInProximity &&
                    (now - previousState.lastCheckTime) > cooldownMillis
        } else {
            isCurrentlyInProximity
        }

        // Update state
        proximityStates[client.id] = ProximityState(
            clientId = client.id,
            wasInProximity = isCurrentlyInProximity,
            lastCheckTime = now
        )

        return isNewEntry
    }

    /**
     * Reset proximity state for a client (useful when meeting ends)
     */
    fun resetProximityState(clientId: String) {
        proximityStates.remove(clientId)
    }

    /**
     * Clear all proximity states
     */
    fun clearAllProximityStates() {
        proximityStates.clear()
    }
}