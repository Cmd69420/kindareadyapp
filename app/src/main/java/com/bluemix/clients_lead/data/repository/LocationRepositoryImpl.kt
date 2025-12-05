package com.bluemix.clients_lead.data.repository

import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.data.mapper.toDomain
import com.bluemix.clients_lead.data.models.LocationLogDto
import com.bluemix.clients_lead.domain.model.LocationLog
import com.bluemix.clients_lead.domain.repository.ILocationRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Updated LocationRepository using REST API instead of Supabase
 */
class LocationRepositoryImpl(
    private val httpClient: HttpClient
) : ILocationRepository {

    override suspend fun insertLocationLog(
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double?,
        battery: Int?
    ): AppResult<LocationLog> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.post(ApiEndpoints.Location.LOGS) {
                setBody(
                    CreateLocationRequest(
                        latitude = latitude,
                        longitude = longitude,
                        accuracy = accuracy,
                        battery = battery
                    )
                )
            }.body<CreateLocationResponse>()

            // Convert backend response to domain model
            response.log.toLocationLogDto().toDomain()
        }
    }

    override suspend fun getLocationLogs(
        userId: String,
        limit: Int
    ): AppResult<List<LocationLog>> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.get(ApiEndpoints.Location.LOGS) {
                parameter("limit", limit)
            }.body<LocationLogsResponse>()

            response.logs.map { it.toLocationLogDto() }.toDomain()
        }
    }

    override suspend fun getLocationLogsByDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): AppResult<List<LocationLog>> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.get(ApiEndpoints.Location.LOGS) {
                parameter("startDate", startDate)
                parameter("endDate", endDate)
            }.body<LocationLogsResponse>()

            response.logs.map { it.toLocationLogDto() }.toDomain()
        }
    }

    override suspend fun deleteOldLogs(olderThanDays: Int): AppResult<Int> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper = { it.toAppError() }) {
                // Your backend doesn't have delete endpoint yet
                // Return 0 for now, implement later if needed
                0
            }
        }
}

// ==================== Request Models ====================

@Serializable
data class CreateLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val activity: String? = null,
    val notes: String? = null,
    val battery: Int? = null
)

// ==================== Response Models ====================

@Serializable
data class CreateLocationResponse(
    val message: String,
    val log: BackendLocationLog
)

@Serializable
data class LocationLogsResponse(
    val logs: List<BackendLocationLog>,
    val pagination: LocationPaginationData? = null
)

@Serializable
data class BackendLocationLog(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val activity: String? = null,
    val notes: String? = null,
    val timestamp: String,
    val battery: Int? = null
)

@Serializable
data class LocationPaginationData(
    val page: Int,
    val limit: Int
)

// ==================== Mapping Functions ====================

/**
 * Convert backend location log to app's LocationLogDto
 */
fun BackendLocationLog.toLocationLogDto(): LocationLogDto {
    return LocationLogDto(
        id = this.id,
        userId = this.userId,
        latitude = this.latitude,
        longitude = this.longitude,
        accuracy = this.accuracy,
        timestamp = this.timestamp,
        battery = this.battery
    )
}