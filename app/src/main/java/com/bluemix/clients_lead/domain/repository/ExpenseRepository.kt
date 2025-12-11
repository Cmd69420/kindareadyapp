package com.bluemix.clients_lead.domain.repository

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.TripExpense

/**
 * Repository interface for Trip Expense operations
 */
interface ExpenseRepository {

    /**
     * Submit a new trip expense
     */
    suspend fun submitExpense(expense: TripExpense): AppResult<TripExpense>

    /**
     * Get all expenses for the current user
     *
     * @param userId Current user's ID
     * @param startDate Optional start date filter (timestamp)
     * @param endDate Optional end date filter (timestamp)
     * @param transportMode Optional transport mode filter
     * @param clientId Optional client ID filter
     */
    suspend fun getExpenses(
        userId: String,
        startDate: Long? = null,
        endDate: Long? = null,
        transportMode: String? = null,
        clientId: String? = null
    ): AppResult<List<TripExpense>>

    /**
     * Get a single expense by ID
     */
    suspend fun getExpenseById(expenseId: String): AppResult<TripExpense>

    /**
     * Update an existing expense
     */
    suspend fun updateExpense(expense: TripExpense): AppResult<TripExpense>

    /**
     * Delete an expense
     */
    suspend fun deleteExpense(expenseId: String): AppResult<Unit>

    /**
     * Upload a receipt image
     *
     * @param imageData Base64 encoded image data
     * @param fileName Original file name
     * @return URL of uploaded receipt
     */
    suspend fun uploadReceipt(imageData: String, fileName: String): AppResult<String>

    /**
     * Get total amount spent by current user
     */
    suspend fun getTotalExpense(): AppResult<Double>
}