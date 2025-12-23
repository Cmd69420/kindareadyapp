package com.bluemix.clients_lead.features.meeting.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Meeting
import com.bluemix.clients_lead.domain.usecases.*
import com.bluemix.clients_lead.features.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream

data class MeetingUiState(
    val isLoading: Boolean = false,
    val activeMeeting: Meeting? = null,
    val error: String? = null,
    val uploadProgress: Float = 0f,
    val isUploadingAttachments: Boolean = false
)

class MeetingViewModel(
    private val startMeeting: StartMeeting,
    private val endMeeting: EndMeeting,
    private val getActiveMeetingForClient: GetActiveMeetingForClient,
    private val uploadMeetingAttachment: UploadMeetingAttachment,
    private val getCurrentUserId: GetCurrentUserId,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeetingUiState())
    val uiState: StateFlow<MeetingUiState> = _uiState.asStateFlow()

    private val locationManager = LocationManager(context)

    fun checkActiveMeeting(clientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = getActiveMeetingForClient(clientId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeMeeting = result.data,
                        error = null
                    )
                }
                is AppResult.Error -> {
                    Timber.e(result.error.message, "Failed to check active meeting")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message
                    )
                }
            }
        }
    }

    fun startMeeting(
        clientId: String,
        latitude: Double?,
        longitude: Double?,
        accuracy: Double?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = startMeeting.invoke(clientId, latitude, longitude, accuracy)) {
                is AppResult.Success -> {
                    Timber.d("Meeting started successfully: ${result.data.id}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeMeeting = result.data,
                        error = null
                    )
                }
                is AppResult.Error -> {
                    Timber.e(result.error.message, "Failed to start meeting")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message ?: "Failed to start meeting"
                    )
                }
            }
        }
    }

    /**
     * ✅ UPDATED: End meeting with client status update
     */
    fun endMeeting(
        comments: String?,
        clientStatus: String, // ✅ NEW: Client status (active/inactive/completed)
        attachmentUris: List<Uri>
    ) {
        viewModelScope.launch {
            val meeting = _uiState.value.activeMeeting
            if (meeting == null) {
                _uiState.value = _uiState.value.copy(
                    error = "No active meeting to end"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Get current location for end location
            var endLatitude: Double? = null
            var endLongitude: Double? = null
            var endAccuracy: Double? = null

            try {
                val location = locationManager.getLastKnownLocation()
                if (location != null) {
                    endLatitude = location.latitude
                    endLongitude = location.longitude
                    endAccuracy = location.accuracy.toDouble()
                    Timber.d("📍 End location captured: $endLatitude, $endLongitude")
                } else {
                    Timber.w("⚠️ No location available when ending meeting")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get end location")
            }

            // Upload attachments first if any
            val uploadedUrls = mutableListOf<String>()
            if (attachmentUris.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(isUploadingAttachments = true)

                attachmentUris.forEachIndexed { index, uri ->
                    _uiState.value = _uiState.value.copy(
                        uploadProgress = (index.toFloat() / attachmentUris.size)
                    )

                    val uploadResult = uploadAttachment(meeting.id, uri)
                    when (uploadResult) {
                        is AppResult.Success -> uploadedUrls.add(uploadResult.data)
                        is AppResult.Error -> {
                            Timber.e(uploadResult.error.message, "Failed to upload attachment")
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isUploadingAttachments = false,
                    uploadProgress = 0f
                )
            }

            // ✅ End meeting with comments, attachments, location, AND client status
            when (val result = endMeeting.invoke(
                meetingId = meeting.id,
                comments = comments,
                attachments = uploadedUrls.ifEmpty { null },
                clientStatus = clientStatus, // ✅ NEW
                latitude = endLatitude,
                longitude = endLongitude,
                accuracy = endAccuracy
            )) {
                is AppResult.Success -> {
                    Timber.d("Meeting ended successfully: ${result.data.id} | Client status: $clientStatus")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeMeeting = null,
                        error = null
                    )
                }
                is AppResult.Error -> {
                    Timber.e(result.error.message, "Failed to end meeting")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message ?: "Failed to end meeting"
                    )
                }
            }
        }
    }

    private suspend fun uploadAttachment(meetingId: String, uri: Uri): AppResult<String> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return AppResult.Error(
                    com.bluemix.clients_lead.core.common.utils.AppError.Unknown(
                        message = "Failed to open file"
                    )
                )
            }

            val fileBytes = inputStream.readBytes()
            inputStream.close()

            val fileName = uri.lastPathSegment ?: "attachment_${System.currentTimeMillis()}"

            uploadMeetingAttachment(meetingId, fileBytes, fileName)
        } catch (e: Exception) {
            Timber.e(e, "Error reading file from URI")
            AppResult.Error(
                com.bluemix.clients_lead.core.common.utils.AppError.Unknown(
                    message = e.message ?: "Error reading file",
                    cause = e
                )
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetMeetingState() {
        _uiState.value = MeetingUiState()
    }
}