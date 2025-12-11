package com.bluemix.clients_lead.features.map.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.usecases.GetClientsWithLocation
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.bluemix.clients_lead.features.location.LocationTrackingStateManager
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
    val userClockedIn: Boolean = false,
    val isTrackingEnabled: Boolean = false,   // <-- new
    val error: String? = null
)

/**
 * ViewModel for the Map screen.
 *
 * Responsibilities:
 * - Enforce that background location tracking is enabled before loading any clients
 * - React to changes in tracking state via [LocationTrackingStateManager]
 * - Load client data when allowed, and clear it immediately when tracking stops
 */
class MapViewModel(
    private val getClientsWithLocation: GetClientsWithLocation,
    private val getCurrentUserId: GetCurrentUserId,
    private val locationTrackingStateManager: LocationTrackingStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        observeTrackingState()
        refreshTrackingState()
    }

    /**
     * Observe tracking state changes and enforce security rules:
     * - When tracking becomes true → load clients
     * - When tracking becomes false → clear clients and related UI
     */
    private fun observeTrackingState() {
        viewModelScope.launch {
            locationTrackingStateManager.trackingState.collect { isTracking ->
                Timber.d("MapViewModel: tracking state changed = $isTracking")

                _uiState.value = _uiState.value.copy(
                    isTrackingEnabled = isTracking
                )

                if (!isTracking) {
                    // Security guarantee: clients must be cleared as soon as tracking stops
                    Timber.d("Tracking disabled. Clearing clients from UI state.")
                    _uiState.value = _uiState.value.copy(
                        clients = emptyList(),
                        userClockedIn = false,
                        selectedClient = null,
                        isLoading = false,
                        error = null
                    )
                } else {
                    // Tracking just became active → attempt to load clients
                    loadClients()
                }
            }
        }
    }



    /**
     * Explicit refresh of tracking state from the system.
     * Can be triggered from UI (e.g., "Refresh status" button).
     */
    fun refreshTrackingState() {
        Timber.d("MapViewModel: refreshing tracking state from system")
        locationTrackingStateManager.updateTrackingState()
    }

    /**
     * Called when user presses "Enable Location Tracking" in the UI.
     * Delegates to [LocationTrackingStateManager] to start the foreground service.
     */
    fun enableTracking() {
        Timber.d("MapViewModel: enableTracking() requested from UI")
        locationTrackingStateManager.startTracking()
    }

    /**
     * Load clients only if tracking is currently enabled.
     * This method is the central enforcement point for the security requirement.
     */
    fun loadClients() {
        viewModelScope.launch {
            val isTracking = locationTrackingStateManager.isCurrentlyTracking()
            if (!isTracking) {
                Timber.w("Denied client loading: tracking is not enabled.")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    clients = emptyList(),
                    userClockedIn = false,
                    error = "Location tracking must be enabled to view clients."
                )
                return@launch
            }

            Timber.d("Loading clients with location...")
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

            Timber.d("Loading clients with location for user: $userId")

            when (val result = getClientsWithLocation(userId)) {
                is AppResult.Success -> {
                    val clients = result.data
                    val clockedIn = clients.isNotEmpty()

                    Timber.d(
                        "Loaded ${clients.size} clients. Clocked-In = $clockedIn " +
                                "| TrackingEnabled = $isTracking"
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        clients = clients,
                        userClockedIn = clockedIn,
                        error = null
                    )
                }

                is AppResult.Error -> {
                    Timber.e(result.error.message, "Failed to load clients")
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

    /**
     * Public refresh entry point (used by the Refresh top-bar button).
     * Still goes through the tracking enforcement in [loadClients].
     */
    fun refresh() = loadClients()
}
