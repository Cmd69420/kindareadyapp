// domain/repository/IQuickVisitRepository.kt
package com.bluemix.clients_lead.domain.repository

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client

interface IQuickVisitRepository {
    suspend fun createQuickVisit(
        clientId: String,
        visitType: String,
        latitude: Double?,
        longitude: Double?,
        accuracy: Double?,
        notes: String?
    ): AppResult<Client>  // ✅ Changed from AppResult<Unit> to AppResult<Client>
}