package com.bluemix.clients_lead.features.Clients.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.ApiClientProvider
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.core.network.TokenStorage
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.usecases.GetAllClients
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

enum class ClientFilter {
    ALL, ACTIVE, INACTIVE, COMPLETED
}

data class ClientsUiState(
    val isLoading: Boolean = false,
    val clients: List<Client> = emptyList(),
    val filteredClients: List<Client> = emptyList(),
    val selectedFilter: ClientFilter = ClientFilter.ACTIVE,
    val searchQuery: String = "",
    val error: String? = null
)

class ClientsViewModel(
    private val getAllClients: GetAllClients,
    private val tokenStorage: TokenStorage,
    private val getCurrentUserId: GetCurrentUserId
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    init {
        loadClients()
    }

    fun loadClients() {
        viewModelScope.launch {
            Timber.d("Loading clients...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val userId = getCurrentUserId()
            if (userId == null) {
                Timber.e("User not authenticated")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            Timber.d("Loading clients for user: $userId")

            when (val result = getAllClients(userId)) {
                is AppResult.Success -> {
                    val clients = result.data
                    Timber.d("Successfully loaded ${clients.size} clients")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        clients = clients,
                        filteredClients = filterClients(clients, ClientFilter.ACTIVE, "")
                    )
                }

                is AppResult.Error -> {
                    Timber.e(
                        result.error.message,
                        "Failed to load clients: ${result.error.message}"
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message ?: "Failed to load clients"
                    )
                }
            }
        }
    }

    fun setFilter(filter: ClientFilter) {
        val filtered = filterClients(
            _uiState.value.clients,
            filter,
            _uiState.value.searchQuery
        )
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredClients = filtered
        )
    }

    fun searchClients(query: String) {
        val filtered = filterClients(
            _uiState.value.clients,
            _uiState.value.selectedFilter,
            query
        )
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredClients = filtered
        )
    }

    private fun filterClients(
        clients: List<Client>,
        filter: ClientFilter,
        query: String
    ): List<Client> {
        var filtered = when (filter) {
            ClientFilter.ALL -> clients
            ClientFilter.ACTIVE -> clients.filter { it.status == "active" }
            ClientFilter.INACTIVE -> clients.filter { it.status == "inactive" }
            ClientFilter.COMPLETED -> clients.filter { it.status == "completed" }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.email?.contains(query, ignoreCase = true) == true ||
                        it.phone?.contains(query, ignoreCase = true) == true
            }
        }

        return filtered
    }

    fun refresh() {
        loadClients()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun uploadExcelFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                Timber.d("📁 Starting Excel upload...")

                // Read file bytes
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Timber.e("❌ Failed to open file stream")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to open file"
                    )
                    return@launch
                }

                val fileBytes = inputStream.readBytes()
                inputStream.close()
                Timber.d("📦 File size: ${fileBytes.size} bytes")

                // Get token
                val token = tokenStorage.getToken()
                if (token == null) {
                    Timber.e("❌ No auth token found")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Not authenticated. Please log in again."
                    )
                    return@launch
                }

                // Create HTTP client
                val client = ApiClientProvider.create(
                    baseUrl = ApiEndpoints.BASE_URL,
                    tokenStorage = tokenStorage
                )

                // Upload file
                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = "${ApiEndpoints.BASE_URL}${ApiEndpoints.Clients.UPLOAD_EXCEL}",
                    formData = formData {
                        append(
                            "file",
                            fileBytes,
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"clients.xlsx\"")
                                append(HttpHeaders.ContentType, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            }
                        )
                    }
                )

                val responseBody = response.bodyAsText()
                Timber.d("✅ Upload success! Status: ${response.status}")
                Timber.d("📥 Response: $responseBody")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "File uploaded successfully!"
                )

                // Refresh clients list after successful upload
                loadClients()

            } catch (e: ClientRequestException) {
                Timber.e(e, "❌ Upload failed with status: ${e.response.status}")
                val errorBody = try {
                    e.response.bodyAsText()
                } catch (ex: Exception) {
                    "Unable to read error response"
                }
                Timber.e("Error body: $errorBody")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Upload failed: ${e.response.status}"
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Upload failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Upload failed: ${e.message}"
                )
            }
        }
    }
}