// data/models/LocationLogDto.kt
package com.bluemix.clients_lead.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationLogDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val timestamp: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val battery: Int? = null
)

@Serializable
data class LocationLogInsertDto(
    @SerialName("user_id")
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val battery: Int? = null
)
