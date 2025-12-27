package com.bluemix.clients_lead.domain.model

data class TripExpense(
    val id: String,
    val userId: String,
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
    TAXI
}