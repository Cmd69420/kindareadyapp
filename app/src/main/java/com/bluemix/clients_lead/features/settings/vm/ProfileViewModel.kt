package com.bluemix.clients_lead.features.settings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.UserProfile
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.bluemix.clients_lead.domain.usecases.GetLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.GetUserProfile
import com.bluemix.clients_lead.domain.usecases.SaveLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.SignOut
import com.bluemix.clients_lead.features.location.LocationTrackingStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val isTrackingEnabled: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    private val getUserProfile: GetUserProfile,
    private val getCurrentUserId: GetCurrentUserId,
    private val getLocationTrackingPreference: GetLocationTrackingPreference,
    private val saveLocationTrackingPreference: SaveLocationTrackingPreference,
    private val signOut: SignOut,
    private val trackingStateManager: LocationTrackingStateManager     // << changed
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeTrackingState()
    }

    /** Keeps UI toggle in sync with the REAL foreground service state */
    private fun observeTrackingState() {
        viewModelScope.launch {
            // initialize state once
            trackingStateManager.updateTrackingState()
            // automatically track service state forever
            trackingStateManager.trackingState.collectLatest { enabled ->
                _uiState.update { it.copy(isTrackingEnabled = enabled) }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            Timber.d("Loading user profile")
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userId = getCurrentUserId() ?: return@launch run {
                Timber.e("User not authenticated")
                _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
            }

            when (val result = getUserProfile(userId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, profile = result.data)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message ?: "Failed to load profile")
                }
            }
        }
    }

    /** Called when user toggles switch */
    fun toggleLocationTracking(enabled: Boolean) {
        viewModelScope.launch {
            saveLocationTrackingPreference(enabled) // keep storing preference

            if (enabled) {
                Timber.d("Starting tracking from Profile")
                trackingStateManager.startTracking()
            } else {
                Timber.d("Stopping tracking from Profile")
                trackingStateManager.stopTracking()
            }
            // DO NOT manually update uiState here — it now auto-updates from Flow
        }
    }

    fun handleSignOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_uiState.value.isTrackingEnabled) {
                trackingStateManager.stopTracking()   // safe stop on logout
            }
            when (val result = signOut()) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> _uiState.update {
                    it.copy(error = result.error.message ?: "Failed to sign out")
                }
            }
        }
    }

    fun refresh() = loadProfile()
}
