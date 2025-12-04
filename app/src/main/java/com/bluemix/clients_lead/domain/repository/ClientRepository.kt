package com.bluemix.clients_lead.domain.repository


import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client
import okhttp3.MultipartBody
//
//interface IClientRepository {
//
//    /**
//     * Get all clients for a user
//     */
//    suspend fun getAllClients(userId: String): Result<List<Client>>
//
//    /**
//     * Get clients by status
//     */
//    suspend fun getClientsByStatus(
//        userId: String,
//        status: String
//    ): Result<List<Client>>
//
//    /**
//     * Get clients with location (for map display)
//     */
//    suspend fun getClientsWithLocation(userId: String): Result<List<Client>>
//
//    /**
//     * Get single client by ID
//     */
//    suspend fun getClientById(clientId: String): Result<Client>
//
//    /**
//     * Search clients by name
//     */
//    suspend fun searchClients(
//        userId: String,
//        query: String
//    ): Result<List<Client>>
//}


/**
 * Repository interface for client data operations.
 * Uses AppResult for consistent error handling across the app.
 */
interface IClientRepository {

    /**
     * Get all clients for a user
     */
    suspend fun getAllClients(userId: String): AppResult<List<Client>>

    /**
     * Get clients by status
     */
    suspend fun getClientsByStatus(
        userId: String,
        status: String
    ): AppResult<List<Client>>

    /**
     * Get clients with location (for map display)
     */
    suspend fun getClientsWithLocation(userId: String): AppResult<List<Client>>

    /**
     * Get single client by ID
     */
    suspend fun getClientById(clientId: String): AppResult<Client>

    /**
     * Search clients by name
     */
    suspend fun searchClients(
        userId: String,
        query: String
    ): AppResult<List<Client>>

    suspend fun uploadExcelFile(file: ByteArray, token: String): Boolean


}
