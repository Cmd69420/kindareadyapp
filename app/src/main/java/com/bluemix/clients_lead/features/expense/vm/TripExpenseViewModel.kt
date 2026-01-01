package com.bluemix.clients_lead.features.expense.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.common.utils.ImageCompressionUtils
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
    val receiptImages: List<String> = emptyList(),

    // Image processing
    val isProcessingImage: Boolean = false,
    val imageProcessingProgress: String? = null,

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

    fun searchEndLocation(query: String) {
        _uiState.value = _uiState.value.copy(endLocationQuery = query)

        if (query.length < 3) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)

            _uiState.value = _uiState.value.copy(isSearching = true)

            val results = locationSearchRepo.searchPlaces(query)

            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        }
    }

    fun selectEndLocation(location: LocationPlace) {
        _uiState.value = _uiState.value.copy(
            endLocation = location,
            endLocationQuery = location.displayName,
            searchResults = emptyList()
        )
        calculateDistance()
    }

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
    // IMAGE HANDLING - NEW
    // ============================================

    /**
     * Process image from camera or gallery
     * Compresses to WebP and uploads
     */
    fun processAndUploadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingImage = true,
                imageProcessingProgress = "Compressing image...",
                error = null
            )

            try {
                // Compress to WebP Base64
                val result = ImageCompressionUtils.compressToWebP(
                    context = context,
                    uri = uri,
                    maxWidth = 1280,
                    maxHeight = 1280,
                    quality = 80
                )

                result.fold(
                    onSuccess = { base64String ->
                        Timber.i("✅ Image compressed successfully")

                        // ✅ NO UPLOAD - Just add to local state
                        val currentReceipts = _uiState.value.receiptImages
                        _uiState.value = _uiState.value.copy(
                            receiptImages = currentReceipts + base64String,  // Store base64 directly
                            isProcessingImage = false,
                            imageProcessingProgress = null
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ Image compression failed")
                        _uiState.value = _uiState.value.copy(
                            isProcessingImage = false,
                            imageProcessingProgress = null,
                            error = "Image processing failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception during image processing")
                _uiState.value = _uiState.value.copy(
                    isProcessingImage = false,
                    imageProcessingProgress = null,
                    error = "Error: ${e.message}"
                )
            }
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
        val currentReceipts = _uiState.value.receiptImages
        if (currentReceipts.size < 5) {
            _uiState.value = _uiState.value.copy(
                receiptImages = currentReceipts + uri,
                error = null
            )
        }
    }

    fun removeReceipt(uri: String) {
        _uiState.value = _uiState.value.copy(
            receiptImages = _uiState.value.receiptImages.filter { it != uri }
        )
    }

    // ============================================
    // SUBMIT EXPENSE
    // ============================================

    // Add this to your submitExpense function in TripExpenseViewModel.kt

    fun submitExpense(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = sessionManager.getCurrentUserId()

            // ✅ Enhanced logging
            Timber.d("📝 SUBMIT EXPENSE CALLED")
            Timber.d("  User ID: $userId")
            Timber.d("  Start: ${state.startLocation?.displayName}")
            Timber.d("  End: ${state.endLocation?.displayName}")
            Timber.d("  Distance: ${state.distanceKm}")
            Timber.d("  Amount: ${state.amountSpent}")
            Timber.d("  Receipts: ${state.receiptImages.size}")

            if (userId == null) {
                _uiState.value = state.copy(error = "User not authenticated")
                Timber.e("❌ No user ID")
                return@launch
            }

            if (state.startLocation == null) {
                _uiState.value = state.copy(error = "Start location is required")
                Timber.e("❌ No start location")
                return@launch
            }

            if (state.endLocation == null) {
                _uiState.value = state.copy(error = "End location is required")
                Timber.e("❌ No end location")
                return@launch
            }

            if (state.distanceKm <= 0) {
                _uiState.value = state.copy(error = "Invalid distance")
                Timber.e("❌ Invalid distance: ${state.distanceKm}")
                return@launch
            }

            if (state.amountSpent < 0) {
                _uiState.value = state.copy(error = "Amount cannot be negative")
                Timber.e("❌ Negative amount: ${state.amountSpent}")
                return@launch
            }

            _uiState.value = state.copy(isSubmitting = true, error = null)

            try {
                val expense = TripExpense(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    tripName = null,  // ✅ Single leg trips don't have tripName
                    startLocation = state.startLocation.displayName,
                    endLocation = state.endLocation.displayName,
                    travelDate = state.travelDate,
                    distanceKm = state.distanceKm,
                    transportMode = state.transportMode,
                    amountSpent = state.amountSpent,
                    notes = state.notes.ifBlank { null },
                    receiptImages = state.receiptImages,
                    clientId = null,
                    clientName = null,
                    legs = null  // ✅ Single leg trip
                )

                // ✅ Log the expense object
                Timber.d("📤 Submitting expense: $expense")

                when (val result = submitExpense(expense)) {
                    is AppResult.Success -> {
                        Timber.i("✅ Expense submitted successfully")
                        Timber.d("  Response ID: ${result.data.id}")
                        _uiState.value = TripExpenseUiState(
                            successMessage = "Expense submitted successfully!"
                        )
                        onSuccess()
                    }
                    is AppResult.Error -> {
                        // ✅ Enhanced error logging
                        Timber.e("❌ Submit failed")
                        Timber.e("  Error type: ${result.error::class.simpleName}")
                        Timber.e("  Error message: ${result.error.message}")
                        Timber.e("  Error cause: ${result.error.cause?.message}")

                        // ✅ Show specific error details to user
                        val errorMessage = when (result.error) {
                            is com.bluemix.clients_lead.core.common.utils.AppError.Validation -> {
                                "Validation error: ${result.error.message}"
                            }
                            is com.bluemix.clients_lead.core.common.utils.AppError.Network -> {
                                "Network error: ${result.error.message}"
                            }
                            is com.bluemix.clients_lead.core.common.utils.AppError.Unauthorized -> {
                                "Session expired. Please login again."
                            }
                            else -> result.error.message ?: "Submission failed"
                        }

                        _uiState.value = state.copy(
                            isSubmitting = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception during submit")
                Timber.e("  Exception type: ${e::class.simpleName}")
                Timber.e("  Exception message: ${e.message}")
                Timber.e("  Stack trace: ${e.stackTraceToString()}")

                _uiState.value = state.copy(
                    isSubmitting = false,
                    error = "Error: ${e.message}"
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