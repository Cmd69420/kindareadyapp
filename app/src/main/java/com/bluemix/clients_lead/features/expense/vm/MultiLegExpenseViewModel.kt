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
import com.bluemix.clients_lead.data.repository.DraftExpenseRepository
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.domain.model.TripExpense
import com.bluemix.clients_lead.domain.model.TripLeg
import com.bluemix.clients_lead.domain.usecases.SubmitTripExpenseUseCase
import com.bluemix.clients_lead.domain.model.DraftExpense
import com.bluemix.clients_lead.domain.model.toDraftLocation
import com.bluemix.clients_lead.domain.model.toLocationPlace
import com.bluemix.clients_lead.domain.model.toDraftString
import com.bluemix.clients_lead.domain.model.toTransportMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import com.google.android.gms.maps.model.LatLng

// UI model for a single leg being edited
data class TripLegUiModel(
    val id: String = UUID.randomUUID().toString(),
    val startLocation: LocationPlace? = null,
    val endLocation: LocationPlace? = null,
    val endLocationQuery: String = "",
    val searchResults: List<LocationPlace> = emptyList(),
    val isSearching: Boolean = false,
    val distanceKm: Double = 0.0,
    val transportMode: TransportMode = TransportMode.BUS,
    val amountSpent: Double = 0.0,
    val notes: String = "",
    val legNumber: Int,
    // ✅ NEW: Route visualization data
    val routePolyline: List<LatLng>? = null,
    val estimatedDuration: Int = 0
)

data class MultiLegTripUiState(
    // Trip metadata
    val tripName: String = "",
    val travelDate: Long = System.currentTimeMillis(),

    // Legs
    val legs: List<TripLegUiModel> = listOf(
        TripLegUiModel(legNumber = 1)
    ),
    val currentEditingLegIndex: Int = 0,

    // Totals
    val totalDistanceKm: Double = 0.0,
    val totalAmountSpent: Double = 0.0,

    // Images
    val receiptImages: List<String> = emptyList(),

    // Location loading
    val isLoadingCurrentLocation: Boolean = false,

    // Image processing
    val isProcessingImage: Boolean = false,
    val imageProcessingProgress: String? = null,

    // UI state
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // ✅ NEW: Draft management
    val currentDraftId: String? = null,
    val isSaving: Boolean = false,
    val lastSaved: Long? = null,
    val availableDrafts: List<DraftExpense> = emptyList(),
    val showDraftsList: Boolean = false
) {
    val canSubmit: Boolean
        get() = tripName.isNotBlank()
                && legs.isNotEmpty()
                && legs.all {
            it.startLocation != null &&
                    it.endLocation != null &&
                    it.distanceKm > 0
        }
                && !isSubmitting

    // ✅ NEW: Check if there are unsaved changes
    val hasUnsavedChanges: Boolean
        get() = tripName.isNotBlank() ||
                legs.any {
                    it.startLocation != null ||
                            it.endLocation != null ||
                            it.amountSpent > 0 ||
                            it.notes.isNotBlank()
                } ||
                receiptImages.isNotEmpty()
}
class MultiLegExpenseViewModel(
    private val submitExpense: SubmitTripExpenseUseCase,
    private val sessionManager: SessionManager,
    private val locationSearchRepo: LocationSearchRepository,
    private val draftRepository: DraftExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiLegTripUiState())
    val uiState: StateFlow<MultiLegTripUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autoSaveJob: Job? = null  // ✅ NEW

    init {
        loadDrafts()      // ✅ NEW
        startAutoSave()   // ✅ NEW
    }

    // ============================================
    // LEG MANAGEMENT
    // ============================================

    fun addNewLeg() {
        val currentLegs = _uiState.value.legs
        val lastLeg = currentLegs.lastOrNull()

        // Auto-populate start location from previous leg's end location
        val newLeg = TripLegUiModel(
            legNumber = currentLegs.size + 1,
            startLocation = lastLeg?.endLocation
        )

        _uiState.value = _uiState.value.copy(
            legs = currentLegs + newLeg,
            currentEditingLegIndex = currentLegs.size
        )

        Timber.d("➕ Added leg ${newLeg.legNumber}")
    }

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
                    // Filter only multi-leg drafts
                    val multiLegDrafts = drafts.filter { it.isMultiLeg }
                    _uiState.value = _uiState.value.copy(
                        availableDrafts = multiLegDrafts
                    )
                    Timber.d("📋 Loaded ${multiLegDrafts.size} multi-leg drafts")
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

            if (!state.hasUnsavedChanges) {
                if (showConfirmation) {
                    _uiState.value = state.copy(error = "Nothing to save")
                }
                return@launch
            }

            _uiState.value = state.copy(isSaving = true, error = null)

            try {
                // Convert legs to JSON or use first leg for draft
                val firstLeg = state.legs.firstOrNull()

                val draft = DraftExpense(
                    id = state.currentDraftId ?: UUID.randomUUID().toString(),
                    userId = userId,
                    tripName = state.tripName.ifBlank { null },
                    startLocation = firstLeg?.startLocation?.toDraftLocation(),
                    endLocation = state.legs.lastOrNull()?.endLocation?.toDraftLocation(),
                    travelDate = state.travelDate,
                    distanceKm = state.totalDistanceKm,
                    transportMode = firstLeg?.transportMode?.toDraftString() ?: "BUS",
                    amountSpent = state.totalAmountSpent,
                    notes = "Multi-leg trip: ${state.legs.size} legs",
                    receiptImages = state.receiptImages,
                    isMultiLeg = true,
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
                        Timber.i("💾 Multi-leg draft saved: ${savedDraft.id}")

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
                    if (draft == null || !draft.isMultiLeg) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingCurrentLocation = false,
                            error = "Multi-leg draft not found"
                        )
                        return@launch
                    }

                    // Recreate basic trip structure from draft
                    val newLeg = TripLegUiModel(
                        legNumber = 1,
                        startLocation = draft.startLocation?.toLocationPlace(),
                        endLocation = draft.endLocation?.toLocationPlace(),
                        endLocationQuery = draft.endLocation?.displayName ?: "",
                        distanceKm = draft.distanceKm,
                        transportMode = draft.transportMode.toTransportMode(),
                        amountSpent = draft.amountSpent
                    )

                    _uiState.value = MultiLegTripUiState(
                        currentDraftId = draft.id,
                        tripName = draft.tripName ?: "",
                        travelDate = draft.travelDate,
                        legs = listOf(newLeg),
                        receiptImages = draft.receiptImages,
                        lastSaved = draft.lastModified,
                        availableDrafts = _uiState.value.availableDrafts,
                        isLoadingCurrentLocation = false
                    )

                    Timber.i("📄 Multi-leg draft loaded: ${draft.id}")
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

    fun removeLeg(index: Int) {
        if (_uiState.value.legs.size <= 1) {
            _uiState.value = _uiState.value.copy(error = "Must have at least one leg")
            return
        }

        val updatedLegs = _uiState.value.legs.toMutableList()
        updatedLegs.removeAt(index)

        // Renumber legs
        updatedLegs.forEachIndexed { idx, leg ->
            updatedLegs[idx] = leg.copy(legNumber = idx + 1)
        }

        _uiState.value = _uiState.value.copy(
            legs = updatedLegs,
            currentEditingLegIndex = 0
        )

        recalculateTotals()
        Timber.d("🗑️ Removed leg at index $index")
    }

    fun switchToLeg(index: Int) {
        _uiState.value = _uiState.value.copy(currentEditingLegIndex = index)
    }

    // ============================================
    // LOCATION SEARCH (for current leg)
    // ============================================

    fun loadCurrentLocationForLeg(legIndex: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingCurrentLocation = true,
                error = null
            )

            val location = locationSearchRepo.getCurrentLocation()

            if (location != null) {
                updateLeg(legIndex) { it.copy(startLocation = location) }
                _uiState.value = _uiState.value.copy(isLoadingCurrentLocation = false)
                calculateDistanceForLeg(legIndex)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingCurrentLocation = false,
                    error = "Failed to get current location"
                )
            }
        }
    }

    fun searchEndLocationForLeg(legIndex: Int, query: String) {
        updateLeg(legIndex) { it.copy(endLocationQuery = query) }

        if (query.length < 3) {
            updateLeg(legIndex) { it.copy(searchResults = emptyList()) }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            updateLeg(legIndex) { it.copy(isSearching = true) }

            val results = locationSearchRepo.searchPlaces(query)

            updateLeg(legIndex) {
                it.copy(
                    searchResults = results,
                    isSearching = false
                )
            }
        }
    }

    fun selectEndLocationForLeg(legIndex: Int, location: LocationPlace) {
        updateLeg(legIndex) {
            it.copy(
                endLocation = location,
                endLocationQuery = location.displayName,
                searchResults = emptyList()
            )
        }
        calculateDistanceForLeg(legIndex)
    }

    // ============================================
    // LEG PROPERTY UPDATES
    // ============================================

    fun updateLegTransportMode(legIndex: Int, mode: TransportMode) {
        updateLeg(legIndex) { it.copy(transportMode = mode) }
        calculateDistanceForLeg(legIndex) // Recalculate with new mode
    }

    fun updateLegAmount(legIndex: Int, amount: Double) {
        updateLeg(legIndex) { it.copy(amountSpent = amount) }
        recalculateTotals()
    }

    fun updateLegNotes(legIndex: Int, notes: String) {
        updateLeg(legIndex) { it.copy(notes = notes) }
    }

    // ============================================
    // TRIP METADATA
    // ============================================

    fun updateTripName(name: String) {
        _uiState.value = _uiState.value.copy(tripName = name, error = null)
    }

    // ============================================
    // CALCULATIONS
    // ============================================

    private fun calculateDistanceForLeg(legIndex: Int) {
        val leg = _uiState.value.legs.getOrNull(legIndex) ?: return
        val start = leg.startLocation
        val end = leg.endLocation

        if (start != null && end != null) {
            viewModelScope.launch {
                try {
                    Timber.d("🗺️ Calculating route for leg $legIndex: ${leg.transportMode}")

                    // ✅ Get full route with geometry
                    val routeResult = locationSearchRepo.calculateRouteWithGeometry(
                        start, end, leg.transportMode
                    )

                    updateLeg(legIndex) {
                        it.copy(
                            distanceKm = routeResult.distanceKm,
                            routePolyline = routeResult.routePolyline,
                            estimatedDuration = routeResult.durationMinutes
                        )
                    }
                    recalculateTotals()

                    Timber.d("✅ Leg $legIndex route: ${routeResult.distanceKm} km, ${routeResult.durationMinutes} min")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to calculate route for leg $legIndex")
                    val fallbackDistance = locationSearchRepo.calculateDistanceKm(start, end)
                    updateLeg(legIndex) {
                        it.copy(
                            distanceKm = fallbackDistance,
                            routePolyline = null,
                            estimatedDuration = 0
                        )
                    }
                    recalculateTotals()
                }
            }
        }
    }
    private fun recalculateTotals() {
        val totalDistance = _uiState.value.legs.sumOf { it.distanceKm }
        val totalAmount = _uiState.value.legs.sumOf { it.amountSpent }

        _uiState.value = _uiState.value.copy(
            totalDistanceKm = totalDistance,
            totalAmountSpent = totalAmount
        )
    }

    // ============================================
    // HELPER FUNCTIONS
    // ============================================

    private fun updateLeg(index: Int, transform: (TripLegUiModel) -> TripLegUiModel) {
        val updatedLegs = _uiState.value.legs.toMutableList()
        if (index in updatedLegs.indices) {
            updatedLegs[index] = transform(updatedLegs[index])
            _uiState.value = _uiState.value.copy(legs = updatedLegs)
        }
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

            if (state.tripName.isBlank()) {
                _uiState.value = state.copy(error = "Trip name is required")
                return@launch
            }

            // Validate all legs
            state.legs.forEachIndexed { index, leg ->
                if (leg.startLocation == null) {
                    _uiState.value = state.copy(error = "Leg ${index + 1}: Start location required")
                    return@launch
                }
                if (leg.endLocation == null) {
                    _uiState.value = state.copy(error = "Leg ${index + 1}: End location required")
                    return@launch
                }
                if (leg.distanceKm <= 0) {
                    _uiState.value = state.copy(error = "Leg ${index + 1}: Invalid distance")
                    return@launch
                }
            }

            _uiState.value = state.copy(isSubmitting = true, error = null)

            try {
                val tripLegs = state.legs.map { legUi ->
                    TripLeg(
                        id = legUi.id,
                        startLocation = legUi.startLocation!!.displayName,
                        endLocation = legUi.endLocation!!.displayName,
                        distanceKm = legUi.distanceKm,
                        transportMode = legUi.transportMode,
                        amountSpent = legUi.amountSpent,
                        notes = legUi.notes.ifBlank { null },
                        legNumber = legUi.legNumber
                    )
                }

                val expense = TripExpense(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    tripName = state.tripName,
                    startLocation = tripLegs.first().startLocation,
                    endLocation = tripLegs.last().endLocation,
                    travelDate = state.travelDate,
                    distanceKm = state.totalDistanceKm,
                    transportMode = tripLegs.first().transportMode,
                    amountSpent = state.totalAmountSpent,
                    notes = null,
                    receiptImages = state.receiptImages,
                    clientId = null,
                    clientName = null,
                    legs = tripLegs
                )

                when (val result = submitExpense(expense)) {
                    is AppResult.Success -> {
                        Timber.i("✅ Multi-leg expense submitted: ${tripLegs.size} legs")
                        _uiState.value = MultiLegTripUiState(
                            successMessage = "Trip submitted successfully!"
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
    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()  // ✅ NEW
        searchJob?.cancel()
    }
}