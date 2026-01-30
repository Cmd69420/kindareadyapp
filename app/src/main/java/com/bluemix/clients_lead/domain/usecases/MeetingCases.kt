package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.CreateMeetingRequest
import com.bluemix.clients_lead.domain.model.Meeting
import com.bluemix.clients_lead.domain.model.UpdateMeetingRequest
import com.bluemix.clients_lead.domain.repository.IMeetingRepository

/**
 * Start a new meeting with a client
 */
class StartMeeting(
    private val repository: IMeetingRepository
) {
    suspend operator fun invoke(
        clientId: String,
        latitude: Double?,
        longitude: Double?,
        accuracy: Double?
    ): AppResult<Meeting> {
        val request = CreateMeetingRequest(
            clientId = clientId,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy
        )
        return repository.startMeeting(request)
    }
}

/**
 * End an ongoing meeting with location capture
 */
class EndMeeting(
    private val repository: IMeetingRepository
) {
    suspend operator fun invoke(
        meetingId: String,
        comments: String? = null,
        attachments: List<String>? = null,
        clientStatus: String? = null, // ✅ NEW
        latitude: Double? = null,
        longitude: Double? = null,
        accuracy: Double? = null
    ): AppResult<Meeting> {
        val request = UpdateMeetingRequest(
            status = "COMPLETED",
            comments = comments,
            attachments = attachments,
            clientStatus = clientStatus, // ✅ NEW
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy
        )
        return repository.endMeeting(meetingId, request)
    }
}

/**
 * Get active meeting for a client
 */
class GetActiveMeetingForClient(
    private val repository: IMeetingRepository
) {
    suspend operator fun invoke(clientId: String): AppResult<Meeting?> {
        return repository.getActiveMeetingForClient(clientId)
    }
}

/**
 * Get all meetings for current user
 */
class GetUserMeetings(
    private val repository: IMeetingRepository
) {
    suspend operator fun invoke(userId: String): AppResult<List<Meeting>> {
        return repository.getUserMeetings(userId)
    }
}

/**
 * Upload attachment to a meeting
 */
class UploadMeetingAttachment(
    private val repository: IMeetingRepository
) {
    suspend operator fun invoke(
        meetingId: String,
        fileData: String,
        fileName: String,
        fileType: String,
        fileSizeMB: Double
    ): AppResult<String> {
        return repository.uploadAttachment(
            meetingId = meetingId,
            fileData = fileData,
            fileName = fileName,
            fileType = fileType,
            fileSizeMB = fileSizeMB)
    }
}