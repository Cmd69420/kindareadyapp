package com.bluemix.clients_lead.data.mapper

import com.bluemix.clients_lead.data.models.TripExpenseCreateDto
import com.bluemix.clients_lead.data.models.TripExpenseDto
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.domain.model.TripExpense

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
        receiptUrls = receiptUrls,
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
        receiptUrls = receiptUrls,
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
        else -> TransportMode.BUS // Default
    }
}

/**
 * Convert TransportMode enum to API string
 */
private fun TransportMode.toApiString(): String {
    return when (this) {
        TransportMode.BUS -> "BUS"
        TransportMode.TRAIN -> "TRAIN"
        TransportMode.BIKE -> "BIKE"
        TransportMode.RICKSHAW -> "RICKSHAW"
        TransportMode.CAR -> "CAR"
        TransportMode.TAXI -> "TAXI"
    }
}