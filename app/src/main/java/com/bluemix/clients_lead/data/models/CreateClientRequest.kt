package com.bluemix.clients_lead.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateClientRequest(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val pincode: String? = null,
    val notes: String? = null
)