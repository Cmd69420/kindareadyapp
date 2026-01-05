// data/repository/QuickVisitRepositoryImpl.kt
package com.bluemix.clients_lead.data.repository

import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.repository.IQuickVisitRepository
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber

class QuickVisitRepositoryImpl(
    private val httpClient: HttpClient
) : IQuickVisitRepository {

    override suspend fun createQuickVisit(
        clientId: String,
        visitType: String,
        latitude: Double?,
        longitude: Double?,
        accuracy: Double?,
        notes: String?
    ): AppResult<Client> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            Timber.d("═══════════════════════════════════════")
            Timber.d("🌐 REPOSITORY: Making API request")
            Timber.d("═══════════════════════════════════════")
            Timber.d("📤 Request Data:")
            Timber.d("   - Client ID: $clientId")
            Timber.d("   - Visit Type: $visitType")
            Timber.d("   - Latitude: $latitude")
            Timber.d("   - Longitude: $longitude")
            Timber.d("   - Notes: $notes")

            val httpResponse = httpClient.post("/api/quick-visits") {
                setBody(CreateQuickVisitRequest(
                    clientId = clientId,
                    visitType = visitType,
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    notes = notes
                ))
            }

            Timber.d("📊 HTTP Status: ${httpResponse.status.value}")

            // Log raw response
            val rawBody = httpResponse.bodyAsText()
            Timber.d("═══════════════════════════════════════")
            Timber.d("📥 RAW API RESPONSE:")
            Timber.d("═══════════════════════════════════════")
            Timber.d(rawBody)
            Timber.d("═══════════════════════════════════════")

            // Parse response
            val response = httpResponse.body<QuickVisitResponse>()

            Timber.d("═══════════════════════════════════════")
            Timber.d("📦 PARSED RESPONSE DATA:")
            Timber.d("═══════════════════════════════════════")
            Timber.d("✅ Message: ${response.message}")
            Timber.d("📋 Quick Visit ID: ${response.quickVisit.id}")
            Timber.d("👤 Client Data from Backend:")
            Timber.d("   - ID: ${response.client.id}")
            Timber.d("   - Name: ${response.client.name}")
            Timber.d("   - Last Visit Date: ${response.client.lastVisitDate}")
            Timber.d("   - Last Visit Type: ${response.client.lastVisitType}")
            Timber.d("   - Last Visit Notes: ${response.client.lastVisitNotes}")
            Timber.d("   - Has Location: ${response.client.hasLocation}")
            Timber.d("   - Latitude: ${response.client.latitude}")
            Timber.d("   - Longitude: ${response.client.longitude}")

            // Convert to domain model
            val domainClient = response.client.toClient()

            Timber.d("═══════════════════════════════════════")
            Timber.d("🔄 CONVERTED TO DOMAIN MODEL:")
            Timber.d("═══════════════════════════════════════")
            Timber.d("   - ID: ${domainClient.id}")
            Timber.d("   - Name: ${domainClient.name}")
            Timber.d("   - Last Visit Date: ${domainClient.lastVisitDate}")
            Timber.d("   - Last Visit Notes: ${domainClient.lastVisitNotes}")
            Timber.d("   - Has Location: ${domainClient.hasLocation}")
            Timber.d("   - Status: ${domainClient.status}")
            Timber.d("═══════════════════════════════════════")

            domainClient
        }
    }
}

@Serializable
private data class CreateQuickVisitRequest(
    val clientId: String,
    val visitType: String,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Double?,
    val notes: String?
)

@Serializable
private data class QuickVisitResponse(
    val message: String,
    val quickVisit: QuickVisitData,
    val client: BackendClientData
)

@Serializable
private data class QuickVisitData(
    val id: String,
    val clientId: String,
    val userId: String,
    val visitType: String
)

@Serializable
private data class BackendClientData(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pincode: String? = null,
    val hasLocation: Boolean = false,
    val status: String = "active",
    val notes: String? = null,
    val createdBy: String? = null,  // ✅ CHANGE THIS to nullable
    val createdAt: String,
    val updatedAt: String,
    val lastVisitDate: String? = null,
    val lastVisitType: String? = null,
    val lastVisitNotes: String? = null
)

private fun BackendClientData.toClient() = Client(
    id = id,
    name = name,
    phone = phone,
    email = email,
    address = address,
    latitude = latitude,
    longitude = longitude,
    pincode = pincode,
    hasLocation = hasLocation,
    status = status,
    notes = notes,
    createdBy = createdBy ?: "",  // ✅ ADD null-safe operator with default
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastVisitDate = lastVisitDate,
    lastVisitType = lastVisitType,
    lastVisitNotes = lastVisitNotes
)