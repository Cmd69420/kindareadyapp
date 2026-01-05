package com.bluemix.clients_lead.features.settings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.UserProfile
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.bluemix.clients_lead.domain.usecases.GetLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.GetTotalExpenseUseCase
import com.bluemix.clients_lead.domain.usecases.GetUserProfile
import com.bluemix.clients_lead.domain.usecases.SaveLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.SignOut
import com.bluemix.clients_lead.domain.usecases.UpdateUserProfile
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
    val error: String? = null,
    val totalSpent: Double = 0.0,
    val showNameDialog: Boolean = false,
    val isUpdatingName: Boolean = false
)

class ProfileViewModel(
    private val getUserProfile: GetUserProfile,
    private val getCurrentUserId: GetCurrentUserId,
    private val getLocationTrackingPreference: GetLocationTrackingPreference,
    private val saveLocationTrackingPreference: SaveLocationTrackingPreference,
    private val signOut: SignOut,
    private val trackingStateManager: LocationTrackingStateManager,
    private val getTotalExpense: GetTotalExpenseUseCase,
    private val updateUserProfile: UpdateUserProfile
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeTrackingState()
    }

    private fun observeTrackingState() {
        viewModelScope.launch {
            trackingStateManager.updateTrackingState()
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
                is AppResult.Success -> {
                    val profile = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            showNameDialog = profile.fullName.isNullOrBlank()
                        )
                    }
                    loadTotalExpense()
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.error.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    private fun loadTotalExpense() {
        viewModelScope.launch {
            Timber.d("Loading total expenses")

            when (val result = getTotalExpense()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(totalSpent = result.data) }
                    Timber.d("Total expenses loaded: ${result.data}")
                }

                is AppResult.Error -> {
                    Timber.e("Failed to load total expenses: ${result.error.message}")
                }
            }
        }
    }

    fun toggleLocationTracking(enabled: Boolean) {
        viewModelScope.launch {
            saveLocationTrackingPreference(enabled)

            if (enabled) {
                Timber.d("Starting tracking from Profile")
                trackingStateManager.startTracking()
            } else {
                Timber.d("Stopping tracking from Profile")
                trackingStateManager.stopTracking()
            }
        }
    }

    fun handleSignOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_uiState.value.isTrackingEnabled) {
                trackingStateManager.stopTracking()
            }
            when (val result = signOut()) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> _uiState.update {
                    it.copy(error = result.error.message ?: "Failed to sign out")
                }
            }
        }
    }

    fun refresh() {
        loadProfile()
    }

    fun showNameDialog() {
        _uiState.update { it.copy(showNameDialog = true) }
    }

    fun hideNameDialog() {
        _uiState.update { it.copy(showNameDialog = false) }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: return@launch run {
                _uiState.update {
                    it.copy(
                        isUpdatingName = false,
                        showNameDialog = false,
                        error = "User not authenticated"
                    )
                }
            }

            _uiState.update { it.copy(isUpdatingName = true, error = null) }

            when (
                val result = updateUserProfile(
                    userId = userId,
                    fullName = newName.trim(),
                    department = _uiState.value.profile?.department,
                    workHoursStart = _uiState.value.profile?.workHoursStart,
                    workHoursEnd = _uiState.value.profile?.workHoursEnd
                )
            ) {
                is AppResult.Success -> {
                    Timber.d("✅ Name updated successfully")
                    _uiState.update {
                        it.copy(
                            isUpdatingName = false,
                            showNameDialog = false,
                            profile = result.data,
                            error = null
                        )
                    }
                }

                is AppResult.Error -> {
                    Timber.e("❌ Failed to update name: ${result.error.message}")
                    _uiState.update {
                        it.copy(
                            isUpdatingName = false,
                            showNameDialog = false, // ← Close dialog even on error
                            error = result.error.message ?: "Failed to update name. Please try again."
                        )
                    }
                }
            }
        }
    }
}