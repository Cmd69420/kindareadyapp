package com.bluemix.clients_lead.domain.model

// domain/model/Client.kt

data class Client(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val hasLocation: Boolean,
    val status: String, // active, inactive, completed
    val notes: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
)
