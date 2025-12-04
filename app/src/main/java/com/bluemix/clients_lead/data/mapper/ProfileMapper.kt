package com.bluemix.clients_lead.data.mapper

import com.bluemix.clients_lead.data.models.ProfileDto
import com.bluemix.clients_lead.domain.model.UserProfile


// Mapper
fun ProfileDto.toDomain(): UserProfile {
    return UserProfile(
        userId = id,  // id IS the user_id in your schema
        email = email,
        fullName = fullName,
        department = department,
        workHoursStart = workHoursStart,
        workHoursEnd = workHoursEnd,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}