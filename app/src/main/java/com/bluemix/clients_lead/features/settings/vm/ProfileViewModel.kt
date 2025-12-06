package com.bluemix.clients_lead.features.settings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import com.bluemix.clients_lead.features.location.isTrackingServiceRunning
import kotlinx.coroutines.launch
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.UserProfile
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.bluemix.clients_lead.domain.usecases.GetLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.GetUserProfile
import kotlinx.coroutines.flow.update
import com.bluemix.clients_lead.domain.usecases.SaveLocationTrackingPreference
import com.bluemix.clients_lead.domain.usecases.SignOut
import com.bluemix.clients_lead.features.location.LocationTrackingManager
import timber.log.Timber
import kotlinx.coroutines.flow.update

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

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled = _trackingEnabled.asStateFlow()


    init {
        loadProfile()
        loadTrackingState()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            Timber.d("Loading user profile")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val userId = getCurrentUserId() ?: run {
                Timber.e("User not authenticated")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            when (val result = getUserProfile(userId)) {
                is AppResult.Success -> {
                    Timber.d("Profile loaded successfully")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = result.data
                    )
                }
                is AppResult.Error -> {
                    Timber.e(result.error.cause, "Failed to load profile")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message ?: "Failed to load profile"
                    )
                }
            }
        }
    }

    private fun loadTrackingState() {
        viewModelScope.launch {
            val preference = getLocationTrackingPreference()
            _uiState.value = _uiState.value.copy(isTrackingEnabled = preference.isEnabled)
        }
    }

    fun toggleLocationTracking(enabled: Boolean) {
        viewModelScope.launch {
            saveLocationTrackingPreference(enabled)
            _uiState.value = _uiState.value.copy(isTrackingEnabled = enabled)

            if (enabled) {
                locationTrackingManager.startTracking()
            } else {
                locationTrackingManager.stopTracking()
            }
        }
    }

    fun handleSignOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_uiState.value.isTrackingEnabled) {
                toggleLocationTracking(false)
            }

            when (val result = signOut()) {
                is AppResult.Success -> {
                    Timber.d("Signed out successfully")
                    onSuccess()
                }
                is AppResult.Error -> {
                    Timber.e(result.error.cause, "Sign out failed")
                    _uiState.value = _uiState.value.copy(
                        error = result.error.message ?: "Failed to sign out"
                    )
                }
            }
        }
    }


    fun syncTrackingState(context: Context) {
        val running = isTrackingServiceRunning(context)
        _trackingEnabled.value = running

        _uiState.update { it.copy(isTrackingEnabled = running) } // <-- required
    }






    fun refresh() {
        loadProfile()
    }
}

//private const val TAG = "ProfileViewModel"
//
//data class ProfileUiState(
//    val isLoading: Boolean = false,
//    val profile: UserProfile? = null,
//    val isTrackingEnabled: Boolean = false,
//    val error: String? = null
//)
//
//class ProfileViewModel(
//    private val getUserProfile: GetUserProfile,
//    private val getCurrentUserId: GetCurrentUserId,
//    private val getLocationTrackingPreference: GetLocationTrackingPreference,
//    private val saveLocationTrackingPreference: SaveLocationTrackingPreference,
//    private val signOut: SignOut,
//    private val locationTrackingManager: LocationTrackingManager
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(ProfileUiState())
//    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
//
//    init {
//        Log.d(TAG, "=== ProfileViewModel initialized ===")
//        loadProfile()
//        loadTrackingState()
//    }
//
//    private fun loadProfile() {
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "📱 Step 1: Starting loadProfile()")
//                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
//
//                Log.d(TAG, "📱 Step 2: Calling getCurrentUserId()")
//                val userId = getCurrentUserId()
//                Log.d(TAG, "📱 Step 3: userId = $userId")
//
//                if (userId == null) {
//                    Log.e(TAG, "❌ ERROR: User not authenticated (userId is null)")
//                    _uiState.value = _uiState.value.copy(
//                        isLoading = false,
//                        error = "User not authenticated"
//                    )
//                    return@launch
//                }
//
//                Log.d(TAG, "📱 Step 4: Calling getUserProfile($userId)")
//                val result = getUserProfile(userId)
//                Log.d(TAG, "📱 Step 5: getUserProfile result type = ${result.javaClass.simpleName}")
//
//                when (result) {
//                    is AppResult.Success -> {
//                        Log.d(TAG, "✅ SUCCESS: Profile loaded")
//                        Log.d(TAG, "✅ Profile data: ${result.data}")
//                        _uiState.value = _uiState.value.copy(
//                            isLoading = false,
//                            profile = result.data
//                        )
//                    }
//                    is AppResult.Error -> {
//                        Log.e(TAG, "❌ ERROR: Failed to load profile")
//                        Log.e(TAG, "❌ Error message: ${result.error.message}")
//                        Log.e(TAG, "❌ Error throwable: ${result.error.cause?.message}")
//                        result.error.cause?.printStackTrace()
//
//                        _uiState.value = _uiState.value.copy(
//                            isLoading = false,
//                            error = "Failed to load profile: ${result.error.message}"
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "❌ EXCEPTION in loadProfile: ${e.message}", e)
//                _uiState.value = _uiState.value.copy(
//                    isLoading = false,
//                    error = "Exception: ${e.message}"
//                )
//            }
//        }
//    }
//
//    private fun loadTrackingState() {
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "📍 Loading tracking state...")
//                val preference = getLocationTrackingPreference()
//                Log.d(TAG, "📍 Tracking enabled: ${preference.isEnabled}")
//                _uiState.value = _uiState.value.copy(isTrackingEnabled = preference.isEnabled)
//            } catch (e: Exception) {
//                Log.e(TAG, "❌ Failed to load tracking state: ${e.message}", e)
//            }
//        }
//    }
//
//    fun toggleLocationTracking(enabled: Boolean) {
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "📍 Toggling location tracking to: $enabled")
//                saveLocationTrackingPreference(enabled)
//                _uiState.value = _uiState.value.copy(isTrackingEnabled = enabled)
//
//                if (enabled) {
//                    locationTrackingManager.startTracking()
//                } else {
//                    locationTrackingManager.stopTracking()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "❌ Failed to toggle tracking: ${e.message}", e)
//            }
//        }
//    }
//
//    fun handleSignOut(onSuccess: () -> Unit) {
//        viewModelScope.launch {
//            try {
//                Log.d(TAG, "🚪 Starting sign out...")
//
//                if (_uiState.value.isTrackingEnabled) {
//                    toggleLocationTracking(false)
//                }
//
//                when (val result = signOut()) {
//                    is AppResult.Success -> {
//                        Log.d(TAG, "✅ Signed out successfully")
//                        onSuccess()
//                    }
//                    is AppResult.Error -> {
//                        Log.e(TAG, "❌ Sign out failed: ${result.error.message}")
//                        _uiState.value = _uiState.value.copy(
//                            error = result.error.message ?: "Failed to sign out"
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "❌ Exception during sign out: ${e.message}", e)
//                _uiState.value = _uiState.value.copy(
//                    error = "Failed to sign out: ${e.message}"
//                )
//            }
//        }
//    }
//
//    fun refresh() {
//        Log.d(TAG, "🔄 Refreshing profile...")
//        loadProfile()
//    }
//}
