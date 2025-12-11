package com.bluemix.clients_lead.features.expense.vm

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.SessionManager
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.domain.model.TripExpense
import com.bluemix.clients_lead.domain.usecases.SubmitTripExpenseUseCase
import com.bluemix.clients_lead.domain.usecases.UploadReceiptUseCase
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
    val isSubmitting: Boolean = false,
    val isUploadingReceipt: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class TripExpenseViewModel(
    private val submitExpense: SubmitTripExpenseUseCase,
    private val uploadReceipt: UploadReceiptUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripExpenseUiState())
    val uiState: StateFlow<TripExpenseUiState> = _uiState.asStateFlow()

    fun updateStartLocation(location: String) {
        _uiState.value = _uiState.value.copy(startLocation = location, error = null)
    }

    fun updateEndLocation(location: String) {
        _uiState.value = _uiState.value.copy(endLocation = location, error = null)
    }

    fun updateDistance(distance: Double) {
        _uiState.value = _uiState.value.copy(distanceKm = distance, error = null)
    }

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
        Timber.d("ADD RECEIPT: $uri")
        val currentReceipts = _uiState.value.receiptUrls
        if (currentReceipts.size < 5) {
            _uiState.value = _uiState.value.copy(
                receiptUrls = currentReceipts + uri,
                error = null
            )
        }
    }
    fun removeReceipt(uri: String) {
        Timber.d("REMOVE RECEIPT: $uri")
        val currentReceipts = _uiState.value.receiptUrls
        _uiState.value = _uiState.value.copy(
            receiptUrls = currentReceipts.filter { it != uri }
        )
    }



    private fun uploadReceiptToBackend(uri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingReceipt = true)

            try {
                // TODO: Convert URI to Base64 if needed
                // For now, just store the local URI
                // In production, you'd upload the image here

                Timber.d("Receipt added (local): $uri")
                _uiState.value = _uiState.value.copy(isUploadingReceipt = false)

                // Uncomment when you want to actually upload:
                /*
                when (val result = uploadReceipt(imageBase64, fileName)) {
                    is AppResult.Success -> {
                        // Replace local URI with backend URL
                        val urls = _uiState.value.receiptUrls.toMutableList()
                        urls[urls.indexOf(uri)] = result.data
                        _uiState.value = _uiState.value.copy(
                            receiptUrls = urls,
                            isUploadingReceipt = false
                        )
                    }
                    is AppResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isUploadingReceipt = false,
                            error = "Failed to upload receipt: ${result.error.message}"
                        )
                    }
                }
                */
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload receipt")
                _uiState.value = _uiState.value.copy(
                    isUploadingReceipt = false,
                    error = "Failed to upload receipt"
                )
            }
        }
    }

    fun submitExpense(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = sessionManager.getCurrentUserId()

            Timber.d("SUBMIT STARTED")

            if (userId == null) {
                Timber.e("VALIDATION FAILED: userId is null")
                _uiState.value = state.copy(error = "User not authenticated")
                return@launch
            }

            if (state.startLocation.isBlank()) {
                Timber.e("VALIDATION FAILED: startLocation empty")
                _uiState.value = state.copy(error = "Start location is required")
                return@launch
            }

            if (state.distanceKm <= 0) {
                Timber.e("VALIDATION FAILED: distance <= 0")
                _uiState.value = state.copy(error = "Distance must be greater than 0")
                return@launch
            }

            if (state.amountSpent < 0) {
                Timber.e("VALIDATION FAILED: amountSpent < 0")
                _uiState.value = state.copy(error = "Amount cannot be negative")
                return@launch
            }

            Timber.d("VALIDATION PASSED — calling backend now")

            _uiState.value = state.copy(isSubmitting = true, error = null)

            try {
                val expense = TripExpense(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    startLocation = state.startLocation,
                    endLocation = state.endLocation.ifBlank { null },
                    travelDate = state.travelDate,
                    distanceKm = state.distanceKm,
                    transportMode = state.transportMode,
                    amountSpent = state.amountSpent,
                    notes = state.notes.ifBlank { null },
                    receiptUrls = state.receiptUrls,
                    clientId = null,
                    clientName = null
                )

                Timber.d("CALLING API: receipts = ${state.receiptUrls}")

                when (val result = submitExpense(expense)) {
                    is AppResult.Success -> {
                        Timber.i("SUBMIT SUCCESS: ${result.data.id}")
                        _uiState.value = TripExpenseUiState(successMessage = "Expense submitted successfully!")
                        onSuccess()
                    }
                    is AppResult.Error -> {
                        Timber.e("SUBMIT ERROR: ${result.error.message}")
                        _uiState.value = state.copy(
                            isSubmitting = false,
                            error = result.error.message ?: "Submission failed"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "EXCEPTION DURING SUBMIT")
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