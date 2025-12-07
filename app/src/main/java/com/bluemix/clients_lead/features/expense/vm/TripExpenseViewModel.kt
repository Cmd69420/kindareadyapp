package com.bluemix.clients_lead.features.expense.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.domain.model.TripExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

data class TripExpenseUiState(
    val startLocation: String = "",
    val endLocation: String = "",
    val travelDate: Long = System.currentTimeMillis(),
    val distanceKm: Double = 0.0,
    val transportMode: TransportMode = TransportMode.BUS,
    val amountSpent: Double = 0.0,
    val notes: String = "",
    val receiptUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TripExpenseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TripExpenseUiState())
    val uiState: StateFlow<TripExpenseUiState> = _uiState.asStateFlow()

    fun updateStartLocation(location: String) {
        _uiState.value = _uiState.value.copy(startLocation = location)
    }

    fun updateEndLocation(location: String) {
        _uiState.value = _uiState.value.copy(endLocation = location)
    }

    fun updateDistance(distance: Double) {
        _uiState.value = _uiState.value.copy(distanceKm = distance)
    }

    fun updateTransportMode(mode: TransportMode) {
        _uiState.value = _uiState.value.copy(transportMode = mode)
    }

    fun updateAmount(amount: Double) {
        _uiState.value = _uiState.value.copy(amountSpent = amount)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun addReceipt(uri: String) {
        val currentReceipts = _uiState.value.receiptUrls
        if (currentReceipts.size < 5) {
            _uiState.value = _uiState.value.copy(
                receiptUrls = currentReceipts + uri
            )
        }
    }

    fun removeReceipt(uri: String) {
        _uiState.value = _uiState.value.copy(
            receiptUrls = _uiState.value.receiptUrls.filter { it != uri }
        )
    }

    fun submitExpense() {
        viewModelScope.launch {
            val state = _uiState.value

            // Validation
            if (state.startLocation.isBlank()) {
                _uiState.value = state.copy(error = "Start location is required")
                return@launch
            }

            if (state.distanceKm <= 0) {
                _uiState.value = state.copy(error = "Distance must be greater than 0")
                return@launch
            }

            if (state.amountSpent < 0) {
                _uiState.value = state.copy(error = "Amount cannot be negative")
                return@launch
            }

            _uiState.value = state.copy(isLoading = true, error = null)

            try {
                val expense = TripExpense(
                    id = UUID.randomUUID().toString(),
                    userId = "current_user_id", // Get from auth use case
                    startLocation = state.startLocation,
                    endLocation = state.endLocation.ifBlank { null },
                    travelDate = state.travelDate,
                    distanceKm = state.distanceKm,
                    transportMode = state.transportMode,
                    amountSpent = state.amountSpent,
                    notes = state.notes.ifBlank { null },
                    receiptUrls = state.receiptUrls
                )

                // TODO: Call repository to save expense
                Timber.d("Expense submitted: $expense")

                // Reset form
                _uiState.value = TripExpenseUiState()

            } catch (e: Exception) {
                Timber.e(e, "Failed to submit expense")
                _uiState.value = state.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to submit expense"
                )
            }
        }
    }

    fun resetError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}