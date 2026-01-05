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
import com.bluemix.clients_lead.data.models.CreateClientRequest
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

class ClientRepositoryImpl(
    private val httpClient: HttpClient
) : IClientRepository {

    override suspend fun getAllClients(userId: String): AppResult<List<Client>> =
        withContext(Dispatchers.IO) {
            Log.d("CLIENT_REPO", "📋 Fetching clients...")

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
            Log.d("CLIENT_REPO", "🔍 Local search: $query")

            val response = httpClient.get(ApiEndpoints.Clients.BASE) {
                parameter("search", query)
                parameter("searchMode", "local") // ✅ Explicitly set local mode
            }.body<ClientsResponse>()

            Log.d("CLIENT_REPO", "✅ Found ${response.clients.size} local clients")
            response.clients.map { it.toClientDto() }.toDomain()
        }
    }

    // ✅ NEW: Remote search across all pincodes with optional filters
    override suspend fun searchRemoteClients(
        userId: String,
        query: String,
        filterType: String?,
        filterValue: String?
    ): AppResult<List<Client>> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            Log.d("CLIENT_REPO", "🌐 Remote search: query='$query', filterType='$filterType', filterValue='$filterValue'")

            val response = httpClient.get(ApiEndpoints.Clients.BASE) {
                parameter("search", query)
                parameter("searchMode", "remote") // Remote mode - no pincode filter

                // ✅ Add optional filters
                if (filterType != null && filterValue != null) {
                    parameter("filterType", filterType)
                    parameter("filterValue", filterValue)
                }
            }.body<ClientsResponse>()

            Log.d("CLIENT_REPO", "✅ Found ${response.clients.size} remote clients")
            response.clients.map { it.toClientDto() }.toDomain()
        }
    }



    override suspend fun createClient(
        name: String,
        phone: String?,
        email: String?,
        address: String?,
        pincode: String?,
        notes: String?
    ): AppResult<Client> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            Log.d("CLIENT_REPO", "🔨 Creating client: $name")

            val request = CreateClientRequest(
                name = name,
                phone = phone,
                email = email,
                address = address,
                pincode = pincode,
                notes = notes
            )

            // 👇 Change this line to use MANUAL_CREATE
            val response = httpClient.post(ApiEndpoints.Clients.MANUAL_CREATE) {
                setBody(request)
            }.body<SingleClientResponse>()

            Log.d("CLIENT_REPO", "✅ Client created: ${response.client.id}")

            response.client.toClientDto().toDomain()
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
    val pagination: PaginationData? = null,
    val userPincode: String? = null,
    val filteredByPincode: Boolean? = null
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
    val pincode: String? = null,
    val status: String? = null,
    val notes: String? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // ✅ ADD THESE FIELDS
    val lastVisitDate: String? = null,
    val lastVisitNotes: String? = null
)

@Serializable
data class PaginationData(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

// ==================== Mapping Functions ====================

// ✅ FIXED: Now includes lastVisitDate and lastVisitNotes
fun BackendClient.toClientDto(): ClientDto {
    return ClientDto(
        id = this.id,
        name = this.name,
        email = this.email,
        phone = this.phone,
        address = this.address,
        latitude = this.latitude,
        longitude = this.longitude,
        pincode = this.pincode,
        status = this.status ?: "active",
        notes = this.notes,
        createdBy = this.createdBy ?: "",
        createdAt = this.createdAt ?: "",
        updatedAt = this.updatedAt ?: "",
        hasLocation = (this.latitude != null && this.longitude != null),
        lastVisitDate = this.lastVisitDate,  // ✅ ADDED
        lastVisitNotes = this.lastVisitNotes  // ✅ ADDED
    )
}