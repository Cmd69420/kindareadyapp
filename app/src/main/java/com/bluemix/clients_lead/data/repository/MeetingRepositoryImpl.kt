package com.bluemix.clients_lead.data.repository

import com.bluemix.clients_lead.core.common.extensions.safeApiCall
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.data.mapper.MeetingMapper
import com.bluemix.clients_lead.data.models.MeetingDto
import com.bluemix.clients_lead.domain.model.CreateMeetingRequest
import com.bluemix.clients_lead.domain.model.Meeting
import com.bluemix.clients_lead.domain.model.UpdateMeetingRequest
import com.bluemix.clients_lead.domain.repository.IMeetingRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
private data class MeetingResponse(
    val message: String? = null,
    val meeting: MeetingDto
)

@Serializable
private data class MeetingListResponse(
    val meetings: List<MeetingDto>
)

@Serializable
private data class ActiveMeetingResponse(
    val meeting: MeetingDto? = null
)

// ✅ FIX: Create a proper serializable data class for the upload request
@Serializable
private data class AttachmentUploadRequest(
    val fileData: String,
    val fileName: String,
    val fileType: String,
    val fileSizeMB: Double
)

@Serializable
private data class UploadResponse(
    val message: String,
    val attachment: AttachmentData,
    val totalAttachments: Int,
    val remainingSlots: Int
)

@Serializable
private data class AttachmentData(
    val id: String,
    val fileName: String,
    val fileType: String,
    val sizeMB: Double,
    val uploadedAt: String
)

class MeetingRepositoryImpl(
    private val httpClient: HttpClient
) : IMeetingRepository {

    override suspend fun startMeeting(request: CreateMeetingRequest): AppResult<Meeting> =
        safeApiCall {
            val response: MeetingResponse = httpClient.post("/meetings") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            MeetingMapper.toDomain(response.meeting)
        }

    override suspend fun endMeeting(
        meetingId: String,
        request: UpdateMeetingRequest
    ): AppResult<Meeting> = safeApiCall {
        val response: MeetingResponse = httpClient.put("/meetings/$meetingId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        MeetingMapper.toDomain(response.meeting)
    }

    override suspend fun getActiveMeetingForClient(clientId: String): AppResult<Meeting?> =
        safeApiCall {
            val response: ActiveMeetingResponse =
                httpClient.get("/meetings/active/$clientId").body()
            response.meeting?.let { MeetingMapper.toDomain(it) }
        }

    override suspend fun getUserMeetings(userId: String): AppResult<List<Meeting>> =
        safeApiCall {
            val response: MeetingListResponse = httpClient.get("/meetings/user/$userId").body()
            response.meetings.map { MeetingMapper.toDomain(it) }
        }

    override suspend fun getClientMeetings(clientId: String): AppResult<List<Meeting>> =
        safeApiCall {
            val response: MeetingListResponse = httpClient.get("/meetings/client/$clientId").body()
            response.meetings.map { MeetingMapper.toDomain(it) }
        }

    /**
     * ✅ FIXED: Use a serializable data class instead of Map<String, Any>
     */
    override suspend fun uploadAttachment(
        meetingId: String,
        fileData: String,
        fileName: String,
        fileType: String,
        fileSizeMB: Double
    ): AppResult<String> = safeApiCall {
        Timber.d("🌐 Uploading to API: POST /meetings/$meetingId/attachments")
        Timber.d("   - File name: $fileName")
        Timber.d("   - File type: $fileType")
        Timber.d("   - File size: $fileSizeMB MB")
        Timber.d("   - Base64 length: ${fileData.length} chars")

        // ✅ FIX: Use a proper data class instead of Map
        val requestBody = AttachmentUploadRequest(
            fileData = fileData,
            fileName = fileName,
            fileType = fileType,
            fileSizeMB = fileSizeMB
        )

        try {
            val response: UploadResponse = httpClient.post("/meetings/$meetingId/attachments") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            Timber.d("✅ API Response received:")
            Timber.d("   - Message: ${response.message}")
            Timber.d("   - Attachment ID: ${response.attachment.id}")
            Timber.d("   - Total attachments: ${response.totalAttachments}")
            Timber.d("   - Remaining slots: ${response.remainingSlots}")

            response.attachment.id

        } catch (e: Exception) {
            Timber.e(e, "❌ API call failed:")
            Timber.e("   - Exception: ${e::class.simpleName}")
            Timber.e("   - Message: ${e.message}")
            throw e
        }
    }
}