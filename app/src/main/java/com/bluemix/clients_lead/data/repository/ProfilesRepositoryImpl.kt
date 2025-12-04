package com.bluemix.clients_lead.data.repository

import android.content.Context
import android.util.Log
import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.data.mapper.toDomain
import com.bluemix.clients_lead.data.models.ProfileDto
import com.bluemix.clients_lead.domain.model.LocationTrackingPreference
import com.bluemix.clients_lead.domain.model.UserProfile
import com.bluemix.clients_lead.domain.repository.IProfileRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber

private const val TAG = "ProfileRepository"

/**
 * Updated ProfileRepository using REST API instead of Supabase
 */
class ProfileRepositoryImpl(
    private val httpClient: HttpClient,
    private val context: Context
) : IProfileRepository {

    private val prefs by lazy {
        context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    }

    override suspend fun getProfile(userId: String): AppResult<UserProfile> =
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Getting profile for userId: $userId")

            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.get(ApiEndpoints.Auth.PROFILE)
                    .body<GetProfileResponse>()

                // Convert backend user data to ProfileDto
                response.user.toProfileDto().toDomain()
            }.also { result ->
                when (result) {
                    is AppResult.Success -> Timber.tag(TAG).d("Profile loaded successfully")
                    is AppResult.Error -> Timber.tag(TAG)
                        .e(result.error.cause, "Failed to load profile: ${result.error.message}")
                }
            }
        }

    override suspend fun updateProfile(
        userId: String,
        fullName: String?,
        department: String?,
        workHoursStart: String?,
        workHoursEnd: String?
    ): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.put(ApiEndpoints.Auth.PROFILE) {
                setBody(
                    UpdateProfileRequest(
                        fullName = fullName,
                        department = department,
                        workHoursStart = workHoursStart,
                        workHoursEnd = workHoursEnd
                    )
                )
            }.body<UpdateProfileResponse>()

            // Convert backend profile data to ProfileDto
            response.profile.toProfileDto().toDomain()
        }
    }

    override suspend fun createProfile(
        userId: String,
        email: String?,
        fullName: String?
    ): AppResult<UserProfile> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            // Backend creates profile automatically during signup
            // This method is not needed, but kept for compatibility
            // Just fetch the profile instead
            val response = httpClient.get(ApiEndpoints.Auth.PROFILE)
                .body<GetProfileResponse>()

            response.user.toProfileDto().toDomain()
        }
    }

    override suspend fun getLocationTrackingPreference(): LocationTrackingPreference {
        val isEnabled = prefs.getBoolean("location_tracking_enabled", false)
        return LocationTrackingPreference(isEnabled)
    }

    override suspend fun saveLocationTrackingPreference(enabled: Boolean) {
        prefs.edit().putBoolean("location_tracking_enabled", enabled).apply()
    }
}

// ==================== Request Models ====================

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null
)

// ==================== Response Models ====================

@Serializable
data class GetProfileResponse(
    val user: BackendUserProfile
)

@Serializable
data class UpdateProfileResponse(
    val message: String,
    val profile: BackendProfileData
)

@Serializable
data class BackendUserProfile(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null,
    val createdAt: String? = null
)

@Serializable
data class BackendProfileData(
    val id: String,
    val userId: String,
    val email: String,
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null
)

// ==================== Mapping Functions ====================

/**
 * Convert backend user profile to app's ProfileDto
 */
fun BackendUserProfile.toProfileDto(): ProfileDto {
    return ProfileDto(
        id = this.id,
        email = this.email,
        fullName = this.fullName,
        department = this.department,
        workHoursStart = this.workHoursStart,
        workHoursEnd = this.workHoursEnd,
        createdAt = this.createdAt ?: "",
        updatedAt = null
    )
}

/**
 * Convert backend profile data to app's ProfileDto
 */
fun BackendProfileData.toProfileDto(): ProfileDto {
    return ProfileDto(
        id = this.userId,
        email = this.email,
        fullName = this.fullName,
        department = this.department,
        workHoursStart = this.workHoursStart,
        workHoursEnd = this.workHoursEnd,
        createdAt = "",
        updatedAt = null
    )
}