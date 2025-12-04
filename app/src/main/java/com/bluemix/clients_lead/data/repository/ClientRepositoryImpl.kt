package com.bluemix.clients_lead.data.repository

import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.data.mapper.toDomain
import com.bluemix.clients_lead.data.models.ClientDto
import com.bluemix.clients_lead.domain.model.Client
import io.ktor.client.request.forms.submitFormWithBinaryData
import com.bluemix.clients_lead.domain.repository.IClientRepository
import io.ktor.client.*
import io.ktor.client.request.forms.formData
import io.ktor.client.call.*
import io.ktor.http.Headers
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import android.util.Log
import io.ktor.http.HttpHeaders

/**
 * Updated ClientRepository using REST API instead of Supabase
 */
class ClientRepositoryImpl(
    private val httpClient: HttpClient
) : IClientRepository {

    override suspend fun getAllClients(userId: String): AppResult<List<Client>> =
        withContext(Dispatchers.IO) {
            Log.d("CLIENT_REPO", "🔍 Fetching clients...")

            runAppCatching(mapper = { it.toAppError() }) {
                Log.d("CLIENT_REPO", "📡 Making request to: ${ApiEndpoints.BASE_URL}${ApiEndpoints.Clients.BASE}")

                val response = httpClient.get(ApiEndpoints.Clients.BASE).body<ClientsResponse>()

                Log.d("CLIENT_REPO", "✅ Got ${response.clients.size} clients")

                response.clients.map { it.toClientDto() }.toDomain()
            }
        }

    override suspend fun getClientsByStatus(
        userId: String,
        status: String
    ): AppResult<List<Client>> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.get(ApiEndpoints.Clients.BASE) {
                parameter("status", status)
            }.body<ClientsResponse>()

            response.clients.map { it.toClientDto() }.toDomain()
        }
    }

    override suspend fun getClientsWithLocation(userId: String): AppResult<List<Client>> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.get(ApiEndpoints.Clients.BASE).body<ClientsResponse>()

                // Filter clients that have location (latitude and longitude not null)
                response.clients
                    .filter { it.latitude != null && it.longitude != null }
                    .map { it.toClientDto() }
                    .toDomain()
            }
        }

    override suspend fun getClientById(clientId: String): AppResult<Client> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper ={ it.toAppError() }) {
                val response = httpClient.get(ApiEndpoints.Clients.byId(clientId))
                    .body<SingleClientResponse>()

                response.client.toClientDto().toDomain()
            }
        }

    override suspend fun searchClients(
        userId: String,
        query: String
    ): AppResult<List<Client>> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.get(ApiEndpoints.Clients.BASE) {
                parameter("search", query)
            }.body<ClientsResponse>()

            response.clients.map { it.toClientDto() }.toDomain()
        }
    }

    override suspend fun uploadExcelFile(file: ByteArray, token: String): Boolean {
        return try {
            httpClient.submitFormWithBinaryData(
                url = ApiEndpoints.Clients.UPLOAD_EXCEL,
                formData = formData {
                    append(
                        "file",
                        file,
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=clients.xlsx")
                            append(
                                HttpHeaders.ContentType,
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                        }
                    )
                }
            ) {
                header("Authorization", "Bearer $token")
            }.status.value in 200..299
        } catch (e: Exception) {
            println("Upload failed: ${e.message}")
            false
        }
    }
}

// ==================== Response Models ====================

@Serializable
data class ClientsResponse(
    val clients: List<BackendClient>,
    val pagination: PaginationData? = null
)

@Serializable
data class SingleClientResponse(
    val client: BackendClient
)

@Serializable
data class BackendClient(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String? = null,
    val notes: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class PaginationData(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

// ==================== Mapping Functions ====================

/**
 * Convert backend client model to app's ClientDto
 */
fun BackendClient.toClientDto(): ClientDto {
    return ClientDto(
        id = this.id,
        name = this.name,
        email = this.email,
        phone = this.phone,
        address = this.address,
        latitude = this.latitude,
        longitude = this.longitude,
        status = this.status ?: "active",
        notes = this.notes,
        createdBy = this.createdBy ?: "",
        createdAt = this.createdAt ?: "",
        hasLocation = (this.latitude != null && this.longitude != null),
        updatedAt = this.updatedAt ?: ""

    )
}