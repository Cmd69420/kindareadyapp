package com.bluemix.clients_lead.features.expense.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.SessionManager
import com.bluemix.clients_lead.data.repository.LocationSearchRepository
import com.bluemix.clients_lead.domain.model.LocationPlace
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.domain.model.TripExpense
import com.bluemix.clients_lead.domain.usecases.SubmitTripExpenseUseCase
import com.bluemix.clients_lead.domain.usecases.UploadReceiptUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

data class TripExpenseUiState(
    // Location search
    val startLocation: LocationPlace? = null,
    val endLocation: LocationPlace? = null,
    val endLocationQuery: String = "",
    val searchResults: List<LocationPlace> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingCurrentLocation: Boolean = false,

    // Expense details
    val travelDate: Long = System.currentTimeMillis(),
    val distanceKm: Double = 0.0,
    val transportMode: TransportMode = TransportMode.BUS,
    val amountSpent: Double = 0.0,
    val notes: String = "",
    val receiptUrls: List<String> = emptyList(),

    // UI state
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class TripExpenseViewModel(
    private val submitExpense: SubmitTripExpenseUseCase,
    private val uploadReceipt: UploadReceiptUseCase,
    private val sessionManager: SessionManager,
    private val locationSearchRepo: LocationSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripExpenseUiState())
    val uiState: StateFlow<TripExpenseUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    // ============================================
    // LOCATION SEARCH
    // ============================================

    /**
     * Load current location (for start location)
     */
    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingCurrentLocation = true,
                error = null
            )

            val location = locationSearchRepo.getCurrentLocation()

            if (location != null) {
                _uiState.value = _uiState.value.copy(
                    startLocation = location,
                    isLoadingCurrentLocation = false
                )
                calculateDistance()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingCurrentLocation = false,
                    error = "Failed to get current location. Enable GPS."
                )
            }
        }
    }

    /**
     * Search for end location
     */
    fun searchEndLocation(query: String) {
        _uiState.value = _uiState.value.copy(endLocationQuery = query)

        if (query.length < 3) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        // Cancel previous search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce

            _uiState.value = _uiState.value.copy(isSearching = true)

            val results = locationSearchRepo.searchPlaces(query)

            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    /**
     * Select end location from search results
     */
    fun selectEndLocation(location: LocationPlace) {
        _uiState.value = _uiState.value.copy(
            endLocation = location,
            endLocationQuery = location.displayName,
            searchResults = emptyList()
        )
        calculateDistance()
    }

    /**
     * Auto-calculate distance when both locations are set
     */
    private fun calculateDistance() {
        val start = _uiState.value.startLocation
        val end = _uiState.value.endLocation

        if (start != null && end != null) {
            val distance = locationSearchRepo.calculateDistanceKm(start, end)
            _uiState.value = _uiState.value.copy(distanceKm = distance)
            Timber.d("📏 Distance calculated: $distance km")
        }
    }

    // ============================================
    // EXPENSE DETAILS
    // ============================================

    fun updateTransportMode(mode: TransportMode) {
        _uiState.value = _uiState.value.copy(transportMode = mode, error = null)
    }

    fun updateAmount(amount: Double) {
        _uiState.value = _uiState.value.copy(amountSpent = amount, error = null)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes, error = null)
    }

    fun addReceipt(uri: String) {
        val currentReceipts = _uiState.value.receiptUrls
        if (currentReceipts.size < 5) {
            _uiState.value = _uiState.value.copy(
                receiptUrls = currentReceipts + uri,
                error = null
            )
        }
    }

    fun removeReceipt(uri: String) {
        _uiState.value = _uiState.value.copy(
            receiptUrls = _uiState.value.receiptUrls.filter { it != uri }
        )
    }

    // ============================================
    // SUBMIT EXPENSE
    // ============================================

    fun submitExpense(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = sessionManager.getCurrentUserId()

            // Validation
            if (userId == null) {
                _uiState.value = state.copy(error = "User not authenticated")
                return@launch
            }

            if (state.startLocation == null) {
                _uiState.value = state.copy(error = "Start location is required")
                return@launch
            }

            if (state.endLocation == null) {
                _uiState.value = state.copy(error = "End location is required")
                return@launch
            }

            if (state.distanceKm <= 0) {
                _uiState.value = state.copy(error = "Invalid distance")
                return@launch
            }

            if (state.amountSpent < 0) {
                _uiState.value = state.copy(error = "Amount cannot be negative")
                return@launch
            }

            _uiState.value = state.copy(isSubmitting = true, error = null)

            try {
                val expense = TripExpense(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    startLocation = state.startLocation.displayName,
                    endLocation = state.endLocation.displayName,
                    travelDate = state.travelDate,
                    distanceKm = state.distanceKm,
                    transportMode = state.transportMode,
                    amountSpent = state.amountSpent,
                    notes = state.notes.ifBlank { null },
                    receiptUrls = state.receiptUrls,
                    clientId = null,
                    clientName = null
                )

                when (val result = submitExpense(expense)) {
                    is AppResult.Success -> {
                        Timber.i("✅ Expense submitted: ${result.data.id}")
                        _uiState.value = TripExpenseUiState(
                            successMessage = "Expense submitted successfully!"
                        )
                        onSuccess()
                    }
                    is AppResult.Error -> {
                        Timber.e("❌ Submit failed: ${result.error.message}")
                        _uiState.value = state.copy(
                            isSubmitting = false,
                            error = result.error.message ?: "Submission failed"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception during submit")
                _uiState.value = state.copy(
                    isSubmitting = false,
                    error = e.message ?: "Unexpected error"
                )
            }
        }
    }

    fun resetError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}