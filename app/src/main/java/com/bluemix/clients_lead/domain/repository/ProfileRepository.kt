package com.bluemix.clients_lead.domain.repository

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.UserProfile
import com.bluemix.clients_lead.domain.model.LocationTrackingPreference

interface IProfileRepository {

    suspend fun getProfile(userId: String): AppResult<UserProfile>

    suspend fun updateProfile(
        userId: String,
        fullName: String?,
        department: String?,
        workHoursStart: String?,
        workHoursEnd: String?
    ): AppResult<UserProfile>

    suspend fun createProfile(
        userId: String,
        email: String?,
        fullName: String?
    ): AppResult<UserProfile>

    suspend fun getLocationTrackingPreference(): LocationTrackingPreference

    suspend fun saveLocationTrackingPreference(enabled: Boolean)
}
