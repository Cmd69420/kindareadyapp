package com.bluemix.clients_lead.domain.model

data class LocationLog(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val timestamp: String,
    val createdAt: String,
    val battery: Int
)