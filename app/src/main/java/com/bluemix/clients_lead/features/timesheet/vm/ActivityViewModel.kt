package com.bluemix.clients_lead.features.timesheet.vm
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.domain.model.LocationLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.usecases.GetCurrentUserId
import com.bluemix.clients_lead.domain.usecases.GetLocationLogs
import timber.log.Timber

data class ActivityUiState(
    val isLoading: Boolean = false,
    val logs: List<LocationLog> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for activity/timesheet screen showing location logs.
 *
 * Improvements:
 * - Removed direct Supabase dependency
 * - Uses use cases for data access
 * - Consistent with other ViewModels architecture
 */
class ActivityViewModel(
    private val getLocationLogs: GetLocationLogs,
    private val getCurrentUserId: GetCurrentUserId
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        loadLocationLogs()
    }

    fun loadLocationLogs(limit: Int = 100) {
        viewModelScope.launch {
            Timber.d("Loading location logs...")
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

            Timber.d("Loading location logs for user: $userId")

            when (val result = getLocationLogs(userId, limit)) {
                is AppResult.Success -> {
                    val logs = result.data
                    Timber.d("Successfully loaded ${logs.size} location logs")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        logs = logs
                    )
                }
                is AppResult.Error -> {
                    Timber.e(result.error.message, "Failed to load logs: ${result.error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message ?: "Failed to load logs"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadLocationLogs()
    }
}