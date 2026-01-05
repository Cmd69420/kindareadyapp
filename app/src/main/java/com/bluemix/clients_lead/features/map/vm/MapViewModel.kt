package com.bluemix.clients_lead.features.map.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.model.LocationLog
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
    val currentLocationLog: LocationLog? = null,  // Added for meetings
    val selectedClient: Client? = null,
    val userClockedIn: Boolean = false,
    val isTrackingEnabled: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the Map screen.
 *
 * Responsibilities:
 * - Enforce that background location tracking is enabled before loading any clients
 * - React to changes in tracking state via [LocationTrackingStateManager]
 * - Provide current location for meetings
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
        startLocationPolling()
        observeLocationSettings()
    }

    private fun observeLocationSettings() {
        viewModelScope.launch {
            // Access the monitor through the state manager
            // You'll need to expose it via a property
        }
    }
    override fun onCleared() {
        super.onCleared()
        // 👇 Add cleanup
        locationTrackingStateManager.cleanup()
    }

    /**
     * Poll for location updates every 10 seconds
     */
    private fun startLocationPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Get location using GlobalContext (like you already do in requestCurrentLocation)
                    val locationManager = com.bluemix.clients_lead.features.location.LocationManager(
                        context = org.koin.core.context.GlobalContext.get().get()
                    )

                    val location = locationManager.getLastKnownLocation()
                    location?.let {
                        // Update LatLng for map display
                        val latLng = LatLng(it.latitude, it.longitude)

                        // Update LocationLog for meeting records
                        val locationLog = LocationLog(
                            id = "",
                            userId = getCurrentUserId() ?: "",
                            latitude = it.latitude,
                            longitude = it.longitude,
                            accuracy = it.accuracy.toDouble(),
                            timestamp = System.currentTimeMillis().toString(),
                            createdAt = System.currentTimeMillis().toString(),
                            battery = 0
                        )

                        _uiState.value = _uiState.value.copy(
                            currentLocation = latLng,
                            currentLocationLog = locationLog
                        )

                        Timber.d("📍 Location updated: ${it.latitude}, ${it.longitude}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get current location")
                }

                // Poll every 10 seconds
                kotlinx.coroutines.delay(30000)
            }
        }
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
                        error = null,
                        currentLocation = null,
                        currentLocationLog = null
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
        viewModelScope.launch {
            if (!locationTrackingStateManager.isLocationEnabled()) {
                Timber.w("Cannot start tracking: Location services are OFF")
                return@launch
            }

            Timber.d("Starting location tracking")
            locationTrackingStateManager.startTracking()
        }
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

    /**
     * Request current location from the location manager.
     * This is called when map loads to center on user's position.
     */
    fun requestCurrentLocation() {
        viewModelScope.launch {
            try {
                val locationManager = com.bluemix.clients_lead.features.location.LocationManager(
                    context = org.koin.core.context.GlobalContext.get().get()
                )

                val location = locationManager.getLastKnownLocation()
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    updateCurrentLocation(latLng)
                    Timber.d("Current location updated: $latLng")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get current location")
            }
        }
    }

    fun selectClient(client: Client?) {
        _uiState.value = _uiState.value.copy(selectedClient = client)
    }


    fun updateQuickVisitStatus(clientId: String, visitType: String) {
        viewModelScope.launch {
            try {
                val currentLocation = _uiState.value.currentLocation

                // Call your API service
                val response = apiService.createQuickVisit(
                    clientId = clientId,
                    visitType = visitType,
                    latitude = currentLocation?.latitude,
                    longitude = currentLocation?.longitude,
                    accuracy = null,
                    notes = null
                )

                if (response.isSuccessful) {
                    Timber.d("Quick visit recorded: $visitType for client $clientId")
                    // Refresh clients to update last visit info
                    loadClients()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to record visit"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update quick visit status")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update visit status: ${e.message}"
                )
            }
        }
    }

    /**
     * Public refresh entry point (used by the Refresh top-bar button).
     * Still goes through the tracking enforcement in [loadClients].
     */
    fun refresh() = loadClients()
}