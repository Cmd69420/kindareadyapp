package com.bluemix.clients_lead.data.repository

import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.data.mapper.toCreateDto
import com.bluemix.clients_lead.data.mapper.toDomain
import com.bluemix.clients_lead.data.models.ExpenseResponse
import com.bluemix.clients_lead.data.models.ExpensesListResponse
import com.bluemix.clients_lead.data.models.ReceiptUploadResponse
import com.bluemix.clients_lead.domain.model.TripExpense
import com.bluemix.clients_lead.domain.repository.ExpenseRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber

private const val TAG = "ExpenseRepository"

/**
 * Implementation of ExpenseRepository using REST API
 */
class ExpenseRepositoryImpl(
    private val httpClient: HttpClient
) : ExpenseRepository {

    override suspend fun submitExpense(expense: TripExpense): AppResult<TripExpense> =
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Submitting expense: ${expense.id}")

            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.post(ApiEndpoints.Expenses.BASE) {
                    setBody(expense.toCreateDto())
                }.body<ExpenseResponse>()

                response.expense.toDomain()
            }.also { result ->
                when (result) {
                    is AppResult.Success -> Timber.tag(TAG).d("Expense submitted successfully: ${result.data.id}")
                    is AppResult.Error -> Timber.tag(TAG)
                        .e(result.error.cause, "Failed to submit expense: ${result.error.message}")
                }
            }
        }

    override suspend fun getExpenses(
        userId: String,
        startDate: Long?,
        endDate: Long?,
        transportMode: String?,
        clientId: String?
    ): AppResult<List<TripExpense>> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("Getting expenses for user: $userId")

        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.get(ApiEndpoints.Expenses.MY_EXPENSES) {
                // Add query parameters if provided
                startDate?.let { parameter("startDate", it) }
                endDate?.let { parameter("endDate", it) }
                transportMode?.let { parameter("transportMode", it) }
                clientId?.let { parameter("clientId", it) }
            }.body<ExpensesListResponse>()

            response.expenses.map { it.toDomain() }
        }.also { result ->
            when (result) {
                is AppResult.Success -> Timber.tag(TAG).d("Loaded ${result.data.size} expenses")
                is AppResult.Error -> Timber.tag(TAG)
                    .e(result.error.cause, "Failed to load expenses: ${result.error.message}")
            }
        }
    }

    override suspend fun getExpenseById(expenseId: String): AppResult<TripExpense> =
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Getting expense by ID: $expenseId")

            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.get(ApiEndpoints.Expenses.byId(expenseId))
                    .body<ExpenseResponse>()

                response.expense.toDomain()
            }.also { result ->
                when (result) {
                    is AppResult.Success -> Timber.tag(TAG).d("Expense loaded: ${result.data.id}")
                    is AppResult.Error -> Timber.tag(TAG)
                        .e(result.error.cause, "Failed to load expense: ${result.error.message}")
                }
            }
        }

    override suspend fun updateExpense(expense: TripExpense): AppResult<TripExpense> =
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Updating expense: ${expense.id}")

            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.put(ApiEndpoints.Expenses.byId(expense.id)) {
                    setBody(expense.toCreateDto())
                }.body<ExpenseResponse>()

                response.expense.toDomain()
            }.also { result ->
                when (result) {
                    is AppResult.Success -> Timber.tag(TAG).d("Expense updated: ${result.data.id}")
                    is AppResult.Error -> Timber.tag(TAG)
                        .e(result.error.cause, "Failed to update expense: ${result.error.message}")
                }
            }
        }

    override suspend fun deleteExpense(expenseId: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Deleting expense: $expenseId")

            runAppCatching(mapper = { it.toAppError() }) {
                httpClient.delete(ApiEndpoints.Expenses.byId(expenseId))
                Unit
            }.also { result ->
                when (result) {
                    is AppResult.Success -> Timber.tag(TAG).d("Expense deleted: $expenseId")
                    is AppResult.Error -> Timber.tag(TAG)
                        .e(result.error.cause, "Failed to delete expense: ${result.error.message}")
                }
            }
        }

    override suspend fun uploadReceipt(imageData: String, fileName: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Uploading receipt: $fileName")

            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.post(ApiEndpoints.Expenses.UPLOAD_RECEIPT) {
                    setBody(ReceiptUploadRequest(imageData = imageData, fileName = fileName))
                }.body<ReceiptUploadResponse>()

                response.url
            }.also { result ->
                when (result) {
                    is AppResult.Success -> Timber.tag(TAG).d("Receipt uploaded: ${result.data}")
                    is AppResult.Error -> Timber.tag(TAG)
                        .e(result.error.cause, "Failed to upload receipt: ${result.error.message}")
                }
            }
        }

    override suspend fun getTotalExpense(): AppResult<Double> =
        withContext(Dispatchers.IO) {
            Timber.tag(TAG).d("Getting total expenses")

            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.get(ApiEndpoints.Expenses.MY_TOTAL)
                    .body<Map<String, Double>>()

                response["totalAmount"] ?: 0.0
            }.also { result ->
                when (result) {
                    is AppResult.Success -> Timber.tag(TAG).d("Total expenses: ${result.data}")
                    is AppResult.Error -> Timber.tag(TAG)
                        .e(result.error.cause, "Failed to get total expenses: ${result.error.message}")
                }
            }
        }
}

// Request model for receipt upload
@Serializable
private data class ReceiptUploadRequest(
    val imageData: String,
    val fileName: String
)