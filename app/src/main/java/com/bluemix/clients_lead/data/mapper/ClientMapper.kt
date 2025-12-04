package com.bluemix.clients_lead.data.mapper

import com.bluemix.clients_lead.data.models.ClientDto
import com.bluemix.clients_lead.domain.model.Client

fun ClientDto.toDomain(): Client {
    return Client(
        id = id,
        name = name,
        phone = phone,
        email = email,
        address = address,
        latitude = latitude,
        longitude = longitude,
        hasLocation = hasLocation,
        status = status,
        notes = notes,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun List<ClientDto>.toDomain(): List<Client> {
    return map { it.toDomain() }
}
