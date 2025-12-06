package com.bluemix.clients_lead.features.settings.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.UserProfile
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.bluemix.clients_lead.domain.usecases.GetLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.GetUserProfile
import com.bluemix.clients_lead.domain.usecases.SaveLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.SignOut
import com.bluemix.clients_lead.features.location.LocationTrackingManager
import com.bluemix.clients_lead.features.location.isTrackingServiceRunning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val locationTrackingManager: LocationTrackingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadTrackingState()
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
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, profile = result.data) }
                }
                is AppResult.Error -> {
                    Timber.e(result.error.cause, "Failed to load profile")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.message ?: "Failed to load profile"
                        )
                    }
                }
            }
        }
    }

    private fun loadTrackingState() {
        viewModelScope.launch {
            val pref = getLocationTrackingPreference()
            _uiState.update { it.copy(isTrackingEnabled = pref.isEnabled) }
        }
    }

    fun toggleLocationTracking(enabled: Boolean) {
        viewModelScope.launch {
            // save preference
            saveLocationTrackingPreference(enabled)
            _uiState.update { it.copy(isTrackingEnabled = enabled) }

            // only start/stop service — NO logout logic here
            if (enabled) {
                Timber.d("Starting tracking from Profile screen")
                locationTrackingManager.startTracking()
            } else {
                Timber.d("Stopping tracking from Profile screen")
                locationTrackingManager.stopTracking()
            }
        }
    }

    // keeps UI toggle in sync with real service state
    fun syncTrackingState(context: Context) {
        val running = isTrackingServiceRunning(context)
        _uiState.update { it.copy(isTrackingEnabled = running) }
    }

    fun handleSignOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // stop tracking ONLY during sign-out
            if (_uiState.value.isTrackingEnabled) {
                toggleLocationTracking(false)
            }

            when (val result = signOut()) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> {
                    Timber.e(result.error.cause, "Sign out failed")
                    _uiState.update {
                        it.copy(error = result.error.message ?: "Failed to sign out")
                    }
                }
            }
        }
    }

    fun refresh() = loadProfile()
}
