package com.bluemix.clients_lead.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer object for Meeting from API
 * Matches backend response format with separate start/end location fields
 */
@Serializable
data class MeetingDto(
    @SerialName("id") val id: String,
    @SerialName("clientId") val clientId: String,
    @SerialName("userId") val userId: String,

    // Start location fields
    @SerialName("startTime") val startTime: String,
    @SerialName("startLatitude") val startLatitude: Double? = null,
    @SerialName("startLongitude") val startLongitude: Double? = null,
    @SerialName("startAccuracy") val startAccuracy: Double? = null,

    // End location fields
    @SerialName("endTime") val endTime: String? = null,
    @SerialName("endLatitude") val endLatitude: Double? = null,
    @SerialName("endLongitude") val endLongitude: Double? = null,
    @SerialName("endAccuracy") val endAccuracy: Double? = null,

    // Meeting details
    @SerialName("status") val status: String,
    @SerialName("comments") val comments: String? = null,
    @SerialName("attachments") val attachments: List<String> = emptyList(),

    // Timestamps
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,

    // Optional client info (from JOIN)
    @SerialName("clientName") val clientName: String? = null,
    @SerialName("clientAddress") val clientAddress: String? = null
)