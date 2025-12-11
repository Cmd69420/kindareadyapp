package com.bluemix.clients_lead.domain.repository

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.CreateMeetingRequest
import com.bluemix.clients_lead.domain.model.Meeting
import com.bluemix.clients_lead.domain.model.UpdateMeetingRequest

/**
 * Repository interface for meeting operations
 */
interface IMeetingRepository {

    /**
     * Start a new meeting with a client
     */
    suspend fun startMeeting(request: CreateMeetingRequest): AppResult<Meeting>

    /**
     * End an ongoing meeting
     */
    suspend fun endMeeting(meetingId: String, request: UpdateMeetingRequest): AppResult<Meeting>

    /**
     * Get active meeting for a specific client (if any)
     */
    suspend fun getActiveMeetingForClient(clientId: String): AppResult<Meeting?>

    /**
     * Get all meetings for the current user
     */
    suspend fun getUserMeetings(userId: String): AppResult<List<Meeting>>

    /**
     * Get meetings for a specific client
     */
    suspend fun getClientMeetings(clientId: String): AppResult<List<Meeting>>

    /**
     * Upload attachment for a meeting
     */
    suspend fun uploadAttachment(meetingId: String, fileData: ByteArray, fileName: String): AppResult<String>
}