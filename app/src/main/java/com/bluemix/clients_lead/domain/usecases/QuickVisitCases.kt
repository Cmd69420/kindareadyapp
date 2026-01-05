// domain/usecases/QuickVisitCases.kt
package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.repository.IQuickVisitRepository
import com.bluemix.clients_lead.domain.model.Client
import timber.log.Timber

// domain/usecases/QuickVisitCases.kt
class CreateQuickVisit(
    private val repository: IQuickVisitRepository
) {
    suspend operator fun invoke(
        clientId: String,
        visitType: String,
        latitude: Double?,
        longitude: Double?,
        accuracy: Double?,
        notes: String?
    ): AppResult<Client> {  // ✅ Changed return type
        Timber.d("📝 CreateQuickVisit: Creating quick visit for client: $clientId, type: $visitType")

        // Validation
        val validTypes = listOf("met_success", "not_available", "office_closed", "phone_call")
        if (visitType !in validTypes) {
            return AppResult.Error(
                com.bluemix.clients_lead.core.common.utils.AppError.Validation(
                    message = "Invalid visit type",
                    code = "INVALID_VISIT_TYPE"
                )
            )
        }

        val result = repository.createQuickVisit(
            clientId = clientId,
            visitType = visitType,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            notes = notes
        )

        when (result) {
            is AppResult.Success -> Timber.d("✅ CreateQuickVisit: Success - Updated client returned")
            is AppResult.Error -> Timber.e("❌ CreateQuickVisit: Error - ${result.error.message}")
        }

        return result
    }
}