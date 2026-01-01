package com.bluemix.clients_lead.domain.model

data class TripExpense(
    val id: String,
    val userId: String,
    val tripName: String? = null,  // NEW: For multi-leg trips
    val legs: List<TripLeg>? = null,  // NEW: Multi-leg support
    val startLocation: String,
    val endLocation: String?,
    val travelDate: Long,
    val distanceKm: Double,
    val transportMode: TransportMode,
    val amountSpent: Double,
    val currency: String = "₹",
    val notes: String?,
    val receiptImages: List<String>,
    val clientId: String? = null,
    val clientName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransportMode {
    BUS,
    TRAIN,
    BIKE,
    RICKSHAW,
    CAR,
    TAXI,
    FLIGHT,
    METRO
}

// NEW: Individual leg of a multi-leg journey
data class TripLeg(
    val id: String,
    val startLocation: String,
    val endLocation: String,
    val distanceKm: Double,
    val transportMode: TransportMode,
    val amountSpent: Double,
    val notes: String? = null,
    val legNumber: Int
)