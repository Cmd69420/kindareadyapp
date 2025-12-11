package com.bluemix.clients_lead.features.meeting.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Meeting
import com.bluemix.clients_lead.domain.usecases.*
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

    /**
     * Check if there's an active meeting for a client
     */
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

    /**
     * Start a new meeting
     */
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
     * End the current meeting
     */
    fun endMeeting(
        comments: String?,
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

            // End the meeting with comments and uploaded attachment URLs
            when (val result = endMeeting.invoke(meeting.id, comments, uploadedUrls.ifEmpty { null })) {
                is AppResult.Success -> {
                    Timber.d("Meeting ended successfully: ${result.data.id}")
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

    /**
     * Upload a single attachment
     */
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
    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset meeting state
     */
    fun resetMeetingState() {
        _uiState.value = MeetingUiState()
    }
}