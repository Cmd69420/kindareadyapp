package com.bluemix.clients_lead.data.mapper

import com.bluemix.clients_lead.data.models.TripExpenseCreateDto
import com.bluemix.clients_lead.data.models.TripExpenseDto
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.domain.model.TripExpense
import com.bluemix.clients_lead.data.models.MultiLegExpenseCreateDto
import com.bluemix.clients_lead.domain.model.TripLeg
import com.bluemix.clients_lead.data.models.TripLegDto


/**
 * Maps DTO to Domain model
 */
fun TripExpenseDto.toDomain(): TripExpense {
    return TripExpense(
        id = id,
        userId = userId,
        startLocation = startLocation,
        endLocation = endLocation,
        travelDate = travelDate,
        distanceKm = distanceKm,
        transportMode = transportMode.toTransportMode(),
        amountSpent = amountSpent,
        currency = currency,
        notes = notes,
        receiptImages = receiptImages ?: emptyList(),
        clientId = clientId,
        clientName = clientName,
        createdAt = createdAt.toLongOrNull() ?: System.currentTimeMillis()
    )
}

/**
 * Maps Domain model to Create DTO (for API requests)
 */
fun TripExpense.toCreateDto(): TripExpenseCreateDto {
    return TripExpenseCreateDto(
        startLocation = startLocation,
        endLocation = endLocation,
        travelDate = travelDate,
        distanceKm = distanceKm,
        transportMode = transportMode.toApiString(),
        amountSpent = amountSpent,
        currency = currency,
        notes = notes,
        receiptImages = receiptImages,
        clientId = clientId
    )
}

/**
 * Convert API string to TransportMode enum
 */
private fun String.toTransportMode(): TransportMode {
    return when (this.uppercase()) {
        "BUS" -> TransportMode.BUS
        "TRAIN" -> TransportMode.TRAIN
        "BIKE" -> TransportMode.BIKE
        "RICKSHAW" -> TransportMode.RICKSHAW
        "CAR" -> TransportMode.CAR
        "TAXI" -> TransportMode.TAXI
        "FLIGHT" -> TransportMode.FLIGHT  // NEW
        "METRO" -> TransportMode.METRO    // NEW
        else -> TransportMode.BUS
    }
}

private fun TransportMode.toApiString(): String {
    return when (this) {
        TransportMode.BUS -> "BUS"
        TransportMode.TRAIN -> "TRAIN"
        TransportMode.BIKE -> "BIKE"
        TransportMode.RICKSHAW -> "RICKSHAW"
        TransportMode.CAR -> "CAR"
        TransportMode.TAXI -> "TAXI"
        TransportMode.FLIGHT -> "FLIGHT"  // NEW
        TransportMode.METRO -> "METRO"    // NEW
    }
}

/**
 * NEW: Map TripLeg domain to DTO
 */
fun TripLeg.toDto(): TripLegDto {
    return TripLegDto(
        id = id,
        startLocation = startLocation,
        endLocation = endLocation,
        distanceKm = distanceKm,
        transportMode = transportMode.toApiString(),
        amountSpent = amountSpent,
        notes = notes,
        legNumber = legNumber
    )
}

/**
 * NEW: Map TripLegDto to domain
 */
fun TripLegDto.toDomain(): TripLeg {
    return TripLeg(
        id = id,
        startLocation = startLocation,
        endLocation = endLocation,
        distanceKm = distanceKm,
        transportMode = transportMode.toTransportMode(),
        amountSpent = amountSpent,
        notes = notes,
        legNumber = legNumber
    )
}

/**
 * NEW: Map multi-leg domain expense to create DTO
 */
fun TripExpense.toMultiLegCreateDto(): MultiLegExpenseCreateDto? {
    if (legs.isNullOrEmpty()) return null

    return MultiLegExpenseCreateDto(
        tripName = tripName ?: "Multi-Leg Trip",
        travelDate = travelDate,
        legs = legs.map { it.toDto() },
        totalDistanceKm = legs.sumOf { it.distanceKm },
        totalAmountSpent = legs.sumOf { it.amountSpent },
        currency = currency,
        notes = notes,
        receiptImages = receiptImages,
        clientId = clientId
    )
}