// presentation/map/MapViewModel.kt
package com.bluemix.clients_lead.features.map.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.usecases.GetClientsWithLocation
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class MapUiState(
    val isLoading: Boolean = false,
    val clients: List<Client> = emptyList(),
    val currentLocation: LatLng? = null,
    val selectedClient: Client? = null,
    val error: String? = null
)

/**
 * ViewModel for map screen displaying client locations.
 *
 * Improvements from previous version:
 * - Removed Context dependency (Android framework dependency in ViewModel)
 * - Removed direct Supabase dependency (use repository pattern)
 * - Uses use cases for data access
 * - Consistent with ClientsViewModel architecture
 */
class MapViewModel(
    private val getClientsWithLocation: GetClientsWithLocation,
    private val getCurrentUserId: GetCurrentUserId
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadClients()
    }

    fun loadClients() {
        viewModelScope.launch {
            Timber.d("Loading clients with location...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Get current user ID from auth repository
            val userId = getCurrentUserId()
            if (userId == null) {
                Timber.e("User not authenticated")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            Timber.d("Loading clients with location for user: $userId")

            when (val result = getClientsWithLocation(userId)) {
                is AppResult.Success -> {
                    val clients = result.data
                    Timber.d("Successfully loaded ${clients.size} clients with location")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        clients = clients
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

    fun updateCurrentLocation(location: LatLng) {
        _uiState.value = _uiState.value.copy(currentLocation = location)
    }

    fun selectClient(client: Client?) {
        _uiState.value = _uiState.value.copy(selectedClient = client)
    }

    fun refresh() {
        loadClients()
    }
}
