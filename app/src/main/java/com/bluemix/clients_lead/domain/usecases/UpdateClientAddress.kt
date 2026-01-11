package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.repository.IClientRepository

class UpdateClientAddress(
    private val repository: IClientRepository
) {
    suspend operator fun invoke(
        clientId: String,
        newAddress: String
    ): AppResult<Client> = repository.updateClientAddress(clientId, newAddress)
}