// TripExpenseViewModel.kt - Fixed with proper type conversions
package com.bluemix.clients_lead.features.expense.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.common.utils.ImageCompressionUtils
import com.bluemix.clients_lead.core.network.SessionManager
import com.bluemix.clients_lead.data.repository.DraftExpenseRepository
import com.bluemix.clients_lead.data.repository.LocationSearchRepository
import com.bluemix.clients_lead.domain.model.DraftExpense
import com.bluemix.clients_lead.domain.model.LocationPlace
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.data.repository.RouteResult
import com.bluemix.clients_lead.domain.model.TripExpense
import com.bluemix.clients_lead.domain.model.toDraftLocation  // ✅ Import extension
import com.bluemix.clients_lead.domain.model.toLocationPlace  // ✅ Import extension
import com.bluemix.clients_lead.domain.model.toDraftString    // ✅ Import extension
import com.bluemix.clients_lead.domain.model.toTransportMode  // ✅ Import extension
import com.bluemix.clients_lead.domain.usecases.SubmitTripExpenseUseCase
import com.bluemix.clients_lead.domain.usecases.UploadReceiptUseCase
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

data class TripExpenseUiState(
    // Location data
    val startLocation: LocationPlace? = null,
    val endLocation: LocationPlace? = null,
    val endLocationQuery: String = "",
    val searchResults: List<LocationPlace> = emptyList(),
    val isSearching: Boolean = false,

    // Trip data
    val travelDate: Long = System.currentTimeMillis(),
    val distanceKm: Double = 0.0,
    val estimatedDuration: Int = 0,
    val transportMode: TransportMode = TransportMode.BUS,
    val amountSpent: Double = 0.0,
    val notes: String = "",

    // Route visualization
    val routePolyline: List<LatLng>? = null,

    // Receipts
    val receiptImages: List<String> = emptyList(),

    // UI states
    val isLoadingCurrentLocation: Boolean = false,
    val isProcessingImage: Boolean = false,
    val imageProcessingProgress: String? = null,
    val isSubmitting: Boolean = false,

    // Draft management
    val currentDraftId: String? = null,
    val isSaving: Boolean = false,
    val lastSaved: Long? = null,
    val availableDrafts: List<DraftExpense> = emptyList(),
    val showDraftsList: Boolean = false,

    // Messages
    val error: String? = null,
    val successMessage: String? = null
) {
    val hasUnsavedChanges: Boolean
        get() = startLocation != null ||
                endLocation != null ||
                amountSpent > 0 ||
                notes.isNotBlank() ||
                receiptImages.isNotEmpty()
}

class TripExpenseViewModel(
    private val submitExpense: SubmitTripExpenseUseCase,
    private val uploadReceipt: UploadReceiptUseCase,
    private val sessionManager: SessionManager,
    private val locationSearchRepo: LocationSearchRepository,
    private val draftRepository: DraftExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripExpenseUiState())
    val uiState: StateFlow<TripExpenseUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        loadDrafts()
        startAutoSave()
    }

    // ============================================
    // DRAFT MANAGEMENT
    // ============================================

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // Auto-save every 30 seconds
                if (_uiState.value.hasUnsavedChanges) {
                    saveDraft(showConfirmation = false)
                }
            }
        }
    }

    fun loadDrafts() {
        viewModelScope.launch {
            val userId = sessionManager.getCurrentUserId() ?: return@launch

            draftRepository.getDrafts(userId).fold(
                onSuccess = { drafts ->
                    _uiState.value = _uiState.value.copy(
                        availableDrafts = drafts
                    )
                    Timber.d("📋 Loaded ${drafts.size} drafts")
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load drafts")
                }
            )
        }
    }

    fun saveDraft(showConfirmation: Boolean = true) {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = sessionManager.getCurrentUserId() ?: return@launch

            // Don't save if nothing to save
            if (!state.hasUnsavedChanges) {
                if (showConfirmation) {
                    _uiState.value = state.copy(
                        error = "Nothing to save"
                    )
                }
                return@launch
            }

            _uiState.value = state.copy(isSaving = true, error = null)

            try {
                val draft = DraftExpense(
                    id = state.currentDraftId ?: UUID.randomUUID().toString(),
                    userId = userId,
                    tripName = null,
                    startLocation = state.startLocation?.toDraftLocation(),  // ✅ Convert to DraftLocation
                    endLocation = state.endLocation?.toDraftLocation(),      // ✅ Convert to DraftLocation
                    travelDate = state.travelDate,
                    distanceKm = state.distanceKm,
                    transportMode = state.transportMode.toDraftString(),    // ✅ Convert to String
                    amountSpent = state.amountSpent,
                    notes = state.notes.ifBlank { null },
                    receiptImages = state.receiptImages,
                    isMultiLeg = false,
                    lastModified = System.currentTimeMillis()
                )

                draftRepository.saveDraft(draft).fold(
                    onSuccess = { savedDraft ->
                        _uiState.value = state.copy(
                            currentDraftId = savedDraft.id,
                            isSaving = false,
                            lastSaved = savedDraft.lastModified,
                            successMessage = if (showConfirmation) "Draft saved successfully" else null
                        )
                        loadDrafts()
                        Timber.i("💾 Draft saved: ${savedDraft.id}")

                        if (showConfirmation) {
                            viewModelScope.launch {
                                delay(2000)
                                resetSuccess()
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = state.copy(
                            isSaving = false,
                            error = "Failed to save draft: ${error.message}"
                        )
                        Timber.e(error, "Failed to save draft")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isSaving = false,
                    error = "Error saving draft: ${e.message}"
                )
                Timber.e(e, "Exception while saving draft")
            }
        }
    }

    fun loadDraft(draftId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingCurrentLocation = true,
                error = null
            )

            draftRepository.getDraftById(draftId).fold(
                onSuccess = { draft ->
                    if (draft == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingCurrentLocation = false,
                            error = "Draft not found"
                        )
                        return@launch
                    }

                    _uiState.value = TripExpenseUiState(
                        currentDraftId = draft.id,
                        startLocation = draft.startLocation?.toLocationPlace(),  // ✅ Convert to LocationPlace
                        endLocation = draft.endLocation?.toLocationPlace(),      // ✅ Convert to LocationPlace
                        endLocationQuery = draft.endLocation?.displayName ?: "",
                        travelDate = draft.travelDate,
                        distanceKm = draft.distanceKm,
                        transportMode = draft.transportMode.toTransportMode(),  // ✅ Convert to TransportMode
                        amountSpent = draft.amountSpent,
                        notes = draft.notes ?: "",
                        receiptImages = draft.receiptImages,
                        lastSaved = draft.lastModified,
                        availableDrafts = _uiState.value.availableDrafts, // ✅ Preserve draft list
                        isLoadingCurrentLocation = false
                    )

                    // Recalculate route if both locations exist
                    if (draft.startLocation != null && draft.endLocation != null) {
                        calculateDistance()
                    }

                    Timber.i("📄 Draft loaded: ${draft.id}")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCurrentLocation = false,
                        error = "Failed to load draft: ${error.message}"
                    )
                    Timber.e(error, "Failed to load draft")
                }
            )
        }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            draftRepository.deleteDraft(draftId).fold(
                onSuccess = {
                    loadDrafts()

                    if (_uiState.value.currentDraftId == draftId) {
                        _uiState.value = _uiState.value.copy(currentDraftId = null)
                    }

                    Timber.i("🗑️ Draft deleted: $draftId")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete draft: ${error.message}"
                    )
                    Timber.e(error, "Failed to delete draft")
                }
            )
        }
    }

    fun toggleDraftsList() {
        _uiState.value = _uiState.value.copy(
            showDraftsList = !_uiState.value.showDraftsList
        )
    }

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
                    error = "Failed to get current location. Please enable GPS."
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

    // ============================================
    // CALCULATIONS
    // ============================================

    private fun calculateDistance() {
        val start = _uiState.value.startLocation
        val end = _uiState.value.endLocation

        if (start != null && end != null) {
            viewModelScope.launch {
                try {
                    Timber.d("🗺️ Calculating route: ${_uiState.value.transportMode}")

                    val routeResult = locationSearchRepo.calculateRouteWithGeometry(
                        start, end, _uiState.value.transportMode
                    )

                    _uiState.value = _uiState.value.copy(
                        distanceKm = routeResult.distanceKm,
                        estimatedDuration = routeResult.durationMinutes,
                        routePolyline = routeResult.routePolyline
                    )

                    Timber.d("✅ Route calculated: ${routeResult.distanceKm} km")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to calculate route")
                    val fallbackDistance = locationSearchRepo.calculateDistanceKm(start, end)
                    _uiState.value = _uiState.value.copy(
                        distanceKm = fallbackDistance,
                        routePolyline = null
                    )
                }
            }
        }
    }

    // ============================================
    // UI UPDATES
    // ============================================

    // In TripExpenseViewModel.kt

    fun updateTransportMode(mode: TransportMode) {
        viewModelScope.launch {
            val start = _uiState.value.startLocation
            val end = _uiState.value.endLocation

            if (start != null && end != null) {
                // Validate mode first
                val (isValid, errorMsg) = locationSearchRepo.validateTransportMode(start, end, mode)

                if (!isValid) {
                    _uiState.value = _uiState.value.copy(
                        error = errorMsg,
                        transportMode = TransportMode.BUS // Fallback to bus
                    )
                    return@launch
                }
            }

            _uiState.value = _uiState.value.copy(transportMode = mode, error = null)
            calculateDistance()
        }
    }

    fun updateAmount(amount: Double) {
        _uiState.value = _uiState.value.copy(amountSpent = amount)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    // ============================================
    // IMAGE HANDLING
    // ============================================

    fun processAndUploadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingImage = true,
                imageProcessingProgress = "Compressing image..."
            )

            val result = ImageCompressionUtils.compressToWebP(
                context = context,
                uri = uri,
                maxWidth = 1280,
                maxHeight = 1280,
                quality = 80
            )

            result.fold(
                onSuccess = { base64 ->
                    _uiState.value = _uiState.value.copy(
                        receiptImages = _uiState.value.receiptImages + base64,
                        isProcessingImage = false,
                        imageProcessingProgress = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingImage = false,
                        imageProcessingProgress = null,
                        error = "Image processing failed: ${error.message}"
                    )
                }
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

    fun submitExpense(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = sessionManager.getCurrentUserId()

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

            _uiState.value = state.copy(isSubmitting = true, error = null)

            try {
                val expense = TripExpense(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    tripName = null,
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
                    legs = null
                )

                when (val result = submitExpense(expense)) {
                    is AppResult.Success -> {
                        Timber.i("✅ Expense submitted successfully")

                        // Delete draft after successful submission
                        state.currentDraftId?.let { draftId ->
                            draftRepository.deleteDraft(draftId)
                        }

                        _uiState.value = TripExpenseUiState(
                            successMessage = "Expense submitted successfully!"
                        )
                        onSuccess()
                    }
                    is AppResult.Error -> {
                        _uiState.value = state.copy(
                            isSubmitting = false,
                            error = result.error.message ?: "Submission failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isSubmitting = false,
                    error = e.message ?: "Unexpected error occurred"
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

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        searchJob?.cancel()
    }
}