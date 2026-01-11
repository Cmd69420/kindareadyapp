package com.bluemix.clients_lead.domain.repository

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.Client

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
     * Search clients by name (local search within user's pincode)
     */
    suspend fun searchClients(
        userId: String,
        query: String
    ): AppResult<List<Client>>

    /**
     * ✅ NEW: Search clients across ALL pincodes (remote search)
     * @param query Search term for name/email/phone
     * @param filterType Type of filter: "pincode", "city", "state", or null
     * @param filterValue Value to filter by (e.g., "400001", "Mumbai", "Maharashtra")
     */
    suspend fun searchRemoteClients(
        userId: String,
        query: String,
        filterType: String? = null,
        filterValue: String? = null
    ): AppResult<List<Client>>


    suspend fun createClient(
        name: String,
        phone: String?,
        email: String?,
        address: String?,
        pincode: String?,
        notes: String?
    ): AppResult<Client>

    suspend fun updateClientAddress(
        clientId: String,
        newAddress: String
    ): AppResult<Client>

    /**
     * Upload Excel file with clients
     */
    suspend fun uploadExcelFile(file: ByteArray, token: String): Boolean
}