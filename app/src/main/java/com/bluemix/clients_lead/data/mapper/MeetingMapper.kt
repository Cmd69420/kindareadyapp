package com.bluemix.clients_lead.data.mapper

import com.bluemix.clients_lead.data.models.MeetingDto
import com.bluemix.clients_lead.domain.model.Meeting
import com.bluemix.clients_lead.domain.model.MeetingLocation
import com.bluemix.clients_lead.domain.model.MeetingStatus

object MeetingMapper {

    fun toDomain(dto: MeetingDto): Meeting {
        // Create location from start coordinates (primary location)
        val location = if (dto.startLatitude != null && dto.startLongitude != null) {
            MeetingLocation(
                latitude = dto.startLatitude,
                longitude = dto.startLongitude,
                accuracy = dto.startAccuracy
            )
        } else null

        return Meeting(
            id = dto.id,
            clientId = dto.clientId,
            userId = dto.userId,
            startTime = dto.startTime,
            endTime = dto.endTime,
            status = when (dto.status.uppercase()) {
                "IN_PROGRESS" -> MeetingStatus.IN_PROGRESS
                "COMPLETED" -> MeetingStatus.COMPLETED
                "CANCELLED" -> MeetingStatus.CANCELLED
                else -> MeetingStatus.IN_PROGRESS
            },
            comments = dto.comments,
            attachments = dto.attachments,
            location = location,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
}