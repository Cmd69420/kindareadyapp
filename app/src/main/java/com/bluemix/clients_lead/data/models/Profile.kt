package com.bluemix.clients_lead.data.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTO
@Serializable
data class ProfileDto(
    @SerialName("id")
    val id: String,  // Your existing primary key

    @SerialName("email")
    val email: String?,

    @SerialName("full_name")
    val fullName: String?,

    @SerialName("department")
    val department: String?,

    @SerialName("work_hours_start")
    val workHoursStart: String?,

    @SerialName("work_hours_end")
    val workHoursEnd: String?,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String?
)


@Serializable
data class ProfileInsertDto(
    @SerialName("id")
    val id: String,  // This is the user_id from auth.users

    @SerialName("email")
    val email: String?,

    @SerialName("full_name")
    val fullName: String?
)

@Serializable
data class ProfileUpdateDto(
    @SerialName("full_name")
    val fullName: String?,

    @SerialName("department")
    val department: String?,

    @SerialName("work_hours_start")
    val workHoursStart: String?,

    @SerialName("work_hours_end")
    val workHoursEnd: String?
)
