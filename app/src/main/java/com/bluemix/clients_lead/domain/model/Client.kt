package com.bluemix.clients_lead.domain.model

import kotlin.math.*

data class Client(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val pincode: String?, // ✅ NEW: Added pincode field
    val hasLocation: Boolean,
    val status: String, // active, inactive, completed
    val notes: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
) {
    /**
     * Calculate distance from this client to given coordinates (in kilometers)
     * Uses Haversine formula for accurate distance calculation
     */
    fun distanceFrom(fromLat: Double, fromLng: Double): Double? {
        if (latitude == null || longitude == null) return null

        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(latitude - fromLat)
        val dLng = Math.toRadians(longitude - fromLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(fromLat)) * cos(Math.toRadians(latitude)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * Format distance for display
     */
    fun formatDistance(fromLat: Double, fromLng: Double): String? {
        val distance = distanceFrom(fromLat, fromLng) ?: return null

        return when {
            distance < 1.0 -> "${(distance * 1000).toInt()}m away"
            distance < 10.0 -> String.format("%.1f km away", distance)
            else -> "${distance.toInt()} km away"
        }
    }
}