package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.LocationLog
import com.bluemix.clients_lead.domain.repository.ILocationRepository

/**
 * Insert a location log entry
 */
class InsertLocationLog(
    private val repository: ILocationRepository
) {
    suspend operator fun invoke(
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double? = null
    ): AppResult<LocationLog> = repository.insertLocationLog(userId, latitude, longitude, accuracy)
}

/**
 * Get location logs for a user
 */
class GetLocationLogs(
    private val repository: ILocationRepository
) {
    suspend operator fun invoke(
        userId: String,
        limit: Int = 100
    ): AppResult<List<LocationLog>> = repository.getLocationLogs(userId, limit)
}

/**
 * Get location logs within a date range
 */
class GetLocationLogsByDateRange(
    private val repository: ILocationRepository
) {
    suspend operator fun invoke(
        userId: String,
        startDate: String,
        endDate: String
    ): AppResult<List<LocationLog>> = repository.getLocationLogsByDateRange(userId, startDate, endDate)
}

/**
 * Delete old location logs for cleanup
 */
class DeleteOldLocationLogs(
    private val repository: ILocationRepository
) {
    suspend operator fun invoke(olderThanDays: Int): AppResult<Int> =
        repository.deleteOldLogs(olderThanDays)
}
