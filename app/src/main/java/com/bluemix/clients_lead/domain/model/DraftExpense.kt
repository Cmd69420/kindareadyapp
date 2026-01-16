// domain/model/DraftExpense.kt
package com.bluemix.clients_lead.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a saved draft of a trip expense
 */
@Serializable
data class DraftExpense(
    val id: String,
    val userId: String,
    val isDraft: Boolean = true,

    // Trip Information
    val tripName: String? = null,
    val startLocation: DraftLocation? = null,
    val endLocation: DraftLocation? = null,
    val travelDate: Long = System.currentTimeMillis(),

    // Expense Details
    val distanceKm: Double = 0.0,
    val transportMode: String = "BUS",
    val amountSpent: Double = 0.0,
    val notes: String? = null,
    val receiptImages: List<String> = emptyList(),

    // Multi-leg specific
    val isMultiLeg: Boolean = false,
    val legs: List<DraftTripLeg> = emptyList(),

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val clientId: String? = null
)

@Serializable
data class DraftLocation(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class DraftTripLeg(
    val id: String,
    val legNumber: Int,
    val startLocation: DraftLocation? = null,
    val endLocation: DraftLocation? = null,
    val distanceKm: Double = 0.0,
    val transportMode: String = "BUS",
    val amountSpent: Double = 0.0,
    val notes: String? = null
)

/**
 * Extension functions to convert between domain models
 */
fun LocationPlace.toDraftLocation() = DraftLocation(
    displayName = displayName,
    latitude = latitude,
    longitude = longitude
)

fun DraftLocation.toLocationPlace() = LocationPlace(
    displayName = displayName,
    latitude = latitude,
    longitude = longitude
)

fun TransportMode.toDraftString() = this.name

fun String.toTransportMode(): TransportMode = try {
    TransportMode.valueOf(this)
} catch (e: Exception) {
    TransportMode.BUS
}