package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.repository.IClientRepository

class GetAllClients(
    private val repository: IClientRepository
) {
    suspend operator fun invoke(userId: String): AppResult<List<Client>> =
        repository.getAllClients(userId)
}

class GetClientById(
    private val repository: IClientRepository
) {
    suspend operator fun invoke(clientId: String): AppResult<Client> =
        repository.getClientById(clientId)
}

class GetClientsWithLocation(
    private val repository: IClientRepository
) {
    suspend operator fun invoke(userId: String): AppResult<List<Client>> =
        repository.getClientsWithLocation(userId)
}

class SearchClients(
    private val repository: IClientRepository
) {
    suspend operator fun invoke(userId: String, query: String): AppResult<List<Client>> =
        repository.searchClients(userId, query)
}

// ✅ NEW: Remote search use case with filters
class SearchRemoteClients(
    private val repository: IClientRepository
) {
    suspend operator fun invoke(
        userId: String,
        query: String,
        filterType: String? = null,
        filterValue: String? = null
    ): AppResult<List<Client>> =
        repository.searchRemoteClients(userId, query, filterType, filterValue)
}

class CreateClient(
    private val repository: IClientRepository
) {
    suspend operator fun invoke(
        name: String,
        phone: String?,
        email: String?,
        address: String?,
        pincode: String?,
        notes: String?
    ): AppResult<Client> = repository.createClient(
        name = name,
        phone = phone,
        email = email,
        address = address,
        pincode = pincode,
        notes = notes
    )
}