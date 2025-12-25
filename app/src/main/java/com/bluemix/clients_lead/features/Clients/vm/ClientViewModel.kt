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
import com.bluemix.clients_lead.domain.usecases.CreateClient
import com.bluemix.clients_lead.domain.usecases.GetAllClients
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.bluemix.clients_lead.domain.usecases.SearchRemoteClients
import com.bluemix.clients_lead.features.location.LocationTrackingStateManager
import kotlinx.coroutines.flow.update
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

enum class ClientFilter {
    ALL, ACTIVE, INACTIVE, COMPLETED
}

enum class SearchMode {
    LOCAL,   // Search within current pincode
    REMOTE   // Search across all pincodes
}

// ✅ SIMPLIFIED: Only need one sort toggle for local mode
data class ClientsUiState(
    val isLoading: Boolean = false,
    val clients: List<Client> = emptyList(),
    val filteredClients: List<Client> = emptyList(),
    val selectedFilter: ClientFilter = ClientFilter.ACTIVE,
    val searchQuery: String = "",
    val searchMode: SearchMode = SearchMode.LOCAL,
    val remoteResults: List<Client> = emptyList(),
    val sortByDistance: Boolean = false, // ✅ Simple toggle for local mode
    val userLocation: Pair<Double, Double>? = null,
    val isSearching: Boolean = false,
    val isTrackingEnabled: Boolean = false,
    val error: String? = null,
    val isCreating: Boolean = false,
    val createSuccess: Boolean = false,
    val createError: String? = null
)

class ClientsViewModel(
    private val getAllClients: GetAllClients,
    private val searchRemoteClients: SearchRemoteClients,
    private val tokenStorage: TokenStorage,
    private val getCurrentUserId: GetCurrentUserId,
    private val locationTrackingStateManager: LocationTrackingStateManager,
    private val context: Context,
    private val createClient: CreateClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientsUiState())
    val uiState: StateFlow<ClientsUiState> = _uiState.asStateFlow()

    private val locationManager = com.bluemix.clients_lead.features.location.LocationManager(context)

    init {
        observeTrackingState()
        refreshTrackingState()
        fetchUserLocation()
    }

    private fun fetchUserLocation() {
        viewModelScope.launch {
            try {
                if (locationManager.hasLocationPermission() && locationManager.isLocationEnabled()) {
                    val location = locationManager.getLastKnownLocation()
                    location?.let {
                        _uiState.value = _uiState.value.copy(
                            userLocation = Pair(it.latitude, it.longitude)
                        )
                        Timber.d("User location: ${it.latitude}, ${it.longitude}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get user location")
            }
        }
    }

    private fun observeTrackingState() {
        viewModelScope.launch {
            locationTrackingStateManager.trackingState.collect { isTracking ->
                Timber.d("ClientsViewModel: tracking state changed = $isTracking")

                _uiState.value = _uiState.value.copy(
                    isTrackingEnabled = isTracking
                )

                if (!isTracking) {
                    Timber.d("Tracking disabled. Clearing clients from UI state.")
                    _uiState.value = _uiState.value.copy(
                        clients = emptyList(),
                        filteredClients = emptyList(),
                        remoteResults = emptyList(),
                        isLoading = false,
                        error = null
                    )
                } else {
                    loadClients()
                }
            }
        }
    }

    // Inside ClientsViewModel class

    fun resetCreateState() {
        _uiState.update { currentState ->
            currentState.copy(
                createSuccess = false,
                createError = null
            )
        }
    }

    // In ClientsViewModel.kt
    fun createClientAction(
        name: String,
        phone: String?,
        email: String?,
        address: String?,
        pincode: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null, createSuccess = false) }

            try {
                // 👇 Use the injected use case instead of creating new client!
                when (val result = createClient(name, phone, email, address, pincode, notes)) {
                    is AppResult.Success -> {
                        Timber.d("✅ Client created successfully: ${result.data.name}")

                        _uiState.update {
                            it.copy(
                                isCreating = false,
                                createSuccess = true
                            )
                        }

                        // Refresh client list
                        loadClients()
                    }

                    is AppResult.Error -> {
                        Timber.e("❌ Create client failed: ${result.error.message}")

                        _uiState.update {
                            it.copy(
                                isCreating = false,
                                createError = result.error.message ?: "Failed to create client"
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Create client error: ${e.message}")
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        createError = e.message ?: "Failed to create client"
                    )
                }
            }
        }
    }


    fun refreshTrackingState() {
        Timber.d("ClientsViewModel: refreshing tracking state from system")
        locationTrackingStateManager.updateTrackingState()
    }

    fun enableTracking() {
        viewModelScope.launch {
            Timber.d("ClientsViewModel: enableTracking() requested from UI")
            locationTrackingStateManager.startTracking()
        }
    }

    fun loadClients() {
        viewModelScope.launch {
            val isTracking = locationTrackingStateManager.isCurrentlyTracking()
            if (!isTracking) {
                Timber.w("Denied client loading: tracking is not enabled.")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    clients = emptyList(),
                    filteredClients = emptyList(),
                    error = "Location tracking must be enabled to view clients."
                )
                return@launch
            }

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

                    // ✅ Apply sorting if enabled
                    val sorted = if (_uiState.value.sortByDistance) {
                        sortClientsByDistance(clients)
                    } else {
                        clients
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        clients = sorted,
                        filteredClients = filterClients(sorted, ClientFilter.ACTIVE, "")
                    )
                }

                is AppResult.Error -> {
                    Timber.e(result.error.message, "Failed to load clients: ${result.error.message}")
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

    fun setSearchMode(mode: SearchMode) {
        Timber.d("Switching search mode to: $mode")
        _uiState.value = _uiState.value.copy(
            searchMode = mode,
            searchQuery = "",
            remoteResults = emptyList(),
            sortByDistance = false // Reset sort when switching modes
        )
    }

    // ✅ SIMPLIFIED: Toggle distance sorting for LOCAL mode
    fun toggleDistanceSort() {
        val newValue = !_uiState.value.sortByDistance
        Timber.d("Toggle distance sort: $newValue")

        _uiState.value = _uiState.value.copy(
            sortByDistance = newValue
        )

        // Re-apply current filter with new sort
        if (_uiState.value.searchMode == SearchMode.LOCAL) {
            val sorted = if (newValue) {
                sortClientsByDistance(_uiState.value.clients)
            } else {
                _uiState.value.clients
            }

            val filtered = filterClients(
                sorted,
                _uiState.value.selectedFilter,
                _uiState.value.searchQuery
            )

            _uiState.value = _uiState.value.copy(
                filteredClients = filtered
            )
        }
    }

    fun searchClients(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                isSearching = query.isNotBlank() && _uiState.value.searchMode == SearchMode.REMOTE
            )

            when (_uiState.value.searchMode) {
                SearchMode.LOCAL -> {
                    // Local search: filter existing clients
                    val filtered = filterClients(
                        _uiState.value.clients,
                        _uiState.value.selectedFilter,
                        query
                    )
                    _uiState.value = _uiState.value.copy(
                        filteredClients = filtered,
                        isSearching = false
                    )
                }

                SearchMode.REMOTE -> {
                    // Remote search: query backend with smart detection
                    if (query.isBlank()) {
                        _uiState.value = _uiState.value.copy(
                            remoteResults = emptyList(),
                            isSearching = false
                        )
                        return@launch
                    }

                    val userId = getCurrentUserId()
                    if (userId == null) {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error = "User not authenticated"
                        )
                        return@launch
                    }

                    Timber.d("Performing remote search: '$query'")

                    // ✅ Backend will auto-detect if it's pincode or text
                    when (val result = searchRemoteClients(userId, query, null, null)) {
                        is AppResult.Success -> {
                            Timber.d("Remote search found ${result.data.size} clients")

                            // ✅ ALWAYS sort remote results by distance
                            val sorted = sortClientsByDistance(result.data)

                            _uiState.value = _uiState.value.copy(
                                remoteResults = sorted,
                                isSearching = false
                            )
                        }

                        is AppResult.Error -> {
                            Timber.e("Remote search failed: ${result.error.message}")
                            _uiState.value = _uiState.value.copy(
                                isSearching = false,
                                error = "Search failed: ${result.error.message}"
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Sort clients by distance from user
    private fun sortClientsByDistance(clients: List<Client>): List<Client> {
        val userLoc = _uiState.value.userLocation
        if (userLoc == null) {
            Timber.w("Cannot sort by distance: user location not available")
            return clients
        }

        return clients.sortedBy { client ->
            client.distanceFrom(userLoc.first, userLoc.second) ?: Double.MAX_VALUE
        }
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
                Timber.d("📂 Starting Excel upload...")

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

                val token = tokenStorage.getToken()
                if (token == null) {
                    Timber.e("❌ No auth token found")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Not authenticated. Please log in again."
                    )
                    return@launch
                }

                val client = ApiClientProvider.create(
                    baseUrl = ApiEndpoints.BASE_URL,
                    tokenStorage = tokenStorage
                )

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