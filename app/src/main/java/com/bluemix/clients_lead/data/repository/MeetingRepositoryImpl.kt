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
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

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

@Serializable
private data class UploadResponse(
    val message: String? = null,
    val url: String,
    val fileName: String? = null
)

class MeetingRepositoryImpl(
    private val httpClient: HttpClient
) : IMeetingRepository {

    override suspend fun startMeeting(request: CreateMeetingRequest): AppResult<Meeting> =
        safeApiCall {
            val response: MeetingResponse = httpClient.post("/meetings") {
                setBody(request)
            }.body()
            MeetingMapper.toDomain(response.meeting)
        }

    override suspend fun endMeeting(
        meetingId: String,
        request: UpdateMeetingRequest
    ): AppResult<Meeting> = safeApiCall {
        val response: MeetingResponse = httpClient.put("/meetings/$meetingId") {
            setBody(request)
        }.body()
        MeetingMapper.toDomain(response.meeting)
    }

    override suspend fun getActiveMeetingForClient(clientId: String): AppResult<Meeting?> =
        safeApiCall {
            val response: ActiveMeetingResponse = httpClient.get("/meetings/active/$clientId").body()
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

    override suspend fun uploadAttachment(
        meetingId: String,
        fileData: ByteArray,
        fileName: String
    ): AppResult<String> = safeApiCall {
        val response: UploadResponse = httpClient.post("/meetings/$meetingId/attachments") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", fileData, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                )
            )
        }.body()
        response.url
    }
}