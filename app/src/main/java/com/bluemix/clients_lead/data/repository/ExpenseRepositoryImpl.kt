package com.bluemix.clients_lead.data.repository

import android.util.Log
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
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private const val TAG = "EXPENSE_DEBUG"  // ✅ Simple tag

class ExpenseRepositoryImpl(
    private val httpClient: HttpClient
) : ExpenseRepository {

    override suspend fun submitExpense(expense: TripExpense): AppResult<TripExpense> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "📤 SUBMIT EXPENSE STARTED")
            Log.d(TAG, "═══════════════════════════════════════")

            val dto = expense.toCreateDto()
            Log.d(TAG, "📦 Request DTO:")
            Log.d(TAG, "  - Start: ${dto.startLocation}")
            Log.d(TAG, "  - End: ${dto.endLocation}")
            Log.d(TAG, "  - Distance: ${dto.distanceKm} km")
            Log.d(TAG, "  - Amount: ${dto.amountSpent}")
            Log.d(TAG, "  - Transport: ${dto.transportMode}")

            runAppCatching(mapper = { it.toAppError() }) {
                try {
                    Log.d(TAG, "🌐 Making HTTP POST to: ${ApiEndpoints.Expenses.BASE}")

                    val httpResponse = httpClient.post(ApiEndpoints.Expenses.BASE) {
                        setBody(dto)
                    }

                    Log.d(TAG, "📊 HTTP Status: ${httpResponse.status.value}")

                    val responseBody = httpResponse.bodyAsText()
                    Log.d(TAG, "📥 RAW RESPONSE:")
                    Log.d(TAG, responseBody)

                    Log.d(TAG, "🔄 Parsing response...")
                    val response: ExpenseResponse = httpResponse.body()

                    Log.d(TAG, "✅ Response parsed successfully!")
                    Log.d(TAG, "  - Expense ID: ${response.expense.id}")

                    val domainExpense = response.expense.toDomain()
                    Log.d(TAG, "✅ Converted to domain model")
                    Log.d(TAG, "═══════════════════════════════════════")

                    domainExpense

                } catch (e: Exception) {
                    Log.e(TAG, "❌❌❌ EXCEPTION CAUGHT ❌❌❌")
                    Log.e(TAG, "Type: ${e::class.java.simpleName}")
                    Log.e(TAG, "Message: ${e.message}")
                    Log.e(TAG, "Stack trace:")
                    e.printStackTrace()
                    Log.e(TAG, "═══════════════════════════════════════")
                    throw e
                }
            }.also { result ->
                when (result) {
                    is AppResult.Success -> {
                        Log.d(TAG, "🎉 FINAL RESULT: SUCCESS")
                        Log.d(TAG, "  - Expense ID: ${result.data.id}")
                    }
                    is AppResult.Error -> {
                        Log.e(TAG, "💥 FINAL RESULT: ERROR")
                        Log.e(TAG, "  - Error Type: ${result.error::class.java.simpleName}")
                        Log.e(TAG, "  - Error Message: ${result.error.message}")
                        Log.e(TAG, "  - Cause: ${result.error.cause?.message}")
                    }
                }
                Log.d(TAG, "═══════════════════════════════════════")
            }
        }

    override suspend fun getExpenses(
        userId: String,
        startDate: Long?,
        endDate: Long?,
        transportMode: String?,
        clientId: String?
    ): AppResult<List<TripExpense>> = withContext(Dispatchers.IO) {
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.get(ApiEndpoints.Expenses.MY_EXPENSES) {
                startDate?.let { parameter("startDate", it) }
                endDate?.let { parameter("endDate", it) }
                transportMode?.let { parameter("transportMode", it) }
                clientId?.let { parameter("clientId", it) }
            }.body<ExpensesListResponse>()

            response.expenses.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseById(expenseId: String): AppResult<TripExpense> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.get(ApiEndpoints.Expenses.byId(expenseId))
                    .body<ExpenseResponse>()
                response.expense.toDomain()
            }
        }

    override suspend fun updateExpense(expense: TripExpense): AppResult<TripExpense> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.put(ApiEndpoints.Expenses.byId(expense.id)) {
                    setBody(expense.toCreateDto())
                }.body<ExpenseResponse>()
                response.expense.toDomain()
            }
        }

    override suspend fun deleteExpense(expenseId: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper = { it.toAppError() }) {
                httpClient.delete(ApiEndpoints.Expenses.byId(expenseId))
                Unit
            }
        }

    override suspend fun uploadReceipt(imageData: String, fileName: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.post(ApiEndpoints.Expenses.UPLOAD_RECEIPT) {
                    setBody(ReceiptUploadRequest(imageData = imageData, fileName = fileName))
                }.body<ReceiptUploadResponse>()
                response.url
            }
        }

    override suspend fun getTotalExpense(): AppResult<Double> =
        withContext(Dispatchers.IO) {
            runAppCatching(mapper = { it.toAppError() }) {
                val response = httpClient.get(ApiEndpoints.Expenses.MY_TOTAL)
                    .body<Map<String, Double>>()
                response["totalAmount"] ?: 0.0
            }
        }
}

@Serializable
private data class ReceiptUploadRequest(
    val imageData: String,
    val fileName: String
)