package com.bluemix.clients_lead.domain.model

import android.location.Location

/**
 * Domain model for a location place
 */
data class LocationPlace(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Calculate distance between two LocationPlace objects in kilometers
 */
fun LocationPlace.distanceTo(other: LocationPlace): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        this.latitude,
        this.longitude,
        other.latitude,
        other.longitude,
        results
    )
    return (results[0] / 1000.0).round(2)
}

/**
 * Round double to specified decimal places
 */
private fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}