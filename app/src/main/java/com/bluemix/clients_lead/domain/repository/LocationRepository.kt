package com.bluemix.clients_lead.domain.repository

import com.bluemix.clients_lead.domain.model.LocationLog
import com.bluemix.clients_lead.core.common.utils.AppResult
//interface ILocationRepository {
//
//    /**
//     * Insert a new location log entry
//     */
//    suspend fun insertLocationLog(
//        userId: String,
//        latitude: Double,
//        longitude: Double,
//        accuracy: Double? = null
//    ): Result<LocationLog>
//
//    /**
//     * Get location logs for a user
//     */
//    suspend fun getLocationLogs(
//        userId: String,
//        limit: Int = 100
//    ): Result<List<LocationLog>>
//
//    /**
//     * Get location logs within a time range
//     */
//    suspend fun getLocationLogsByDateRange(
//        userId: String,
//        startDate: String,
//        endDate: String
//    ): Result<List<LocationLog>>
//
//    /**
//     * Delete old location logs (for cleanup)
//     */
//    suspend fun deleteOldLogs(olderThanDays: Int): Result<Int>
//}

/**
 * Repository interface for location tracking operations.
 * Uses AppResult for consistent error handling across the app.
 */
interface ILocationRepository {

    /**
     * Insert a new location log entry
     */
    suspend fun insertLocationLog(
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double? = null,
        battery: Int? = null
    ): AppResult<LocationLog>

    /**
     * Get location logs for a user
     */
    suspend fun getLocationLogs(
        userId: String,
        limit: Int = 100
    ): AppResult<List<LocationLog>>

    /**
     * Get location logs within a time range
     */
    suspend fun getLocationLogsByDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): AppResult<List<LocationLog>>

    /**
     * Delete old location logs (for cleanup)
     */
    suspend fun deleteOldLogs(olderThanDays: Int): AppResult<Int>
}
