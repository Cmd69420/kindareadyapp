package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.model.TripExpense
import com.bluemix.clients_lead.domain.repository.ExpenseRepository
import timber.log.Timber

/**
 * Submit a new trip expense
 */
class SubmitTripExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: TripExpense): AppResult<TripExpense> {
        Timber.d("📝 SubmitTripExpense: Submitting expense")

        // Validation
        if (expense.startLocation.isBlank()) {
            Timber.e("❌ Validation failed: Start location is required")
            return AppResult.Error(
                com.bluemix.clients_lead.core.common.utils.AppError.Validation(
                    message = "Start location is required",
                    fieldErrors = mapOf("startLocation" to "This field is required")
                )
            )
        }

        if (expense.distanceKm <= 0) {
            Timber.e("❌ Validation failed: Distance must be greater than 0")
            return AppResult.Error(
                com.bluemix.clients_lead.core.common.utils.AppError.Validation(
                    message = "Distance must be greater than 0",
                    fieldErrors = mapOf("distanceKm" to "Must be greater than 0")
                )
            )
        }

        if (expense.amountSpent < 0) {
            Timber.e("❌ Validation failed: Amount cannot be negative")
            return AppResult.Error(
                com.bluemix.clients_lead.core.common.utils.AppError.Validation(
                    message = "Amount cannot be negative",
                    fieldErrors = mapOf("amountSpent" to "Cannot be negative")
                )
            )
        }

        val result = repository.submitExpense(expense)

        when (result) {
            is AppResult.Success -> Timber.d("✅ SubmitTripExpense: Success - ${result.data.id}")
            is AppResult.Error -> Timber.e("❌ SubmitTripExpense: Error - ${result.error.message}")
        }

        return result
    }
}

/**
 * Get all trip expenses for a user
 */
class GetTripExpensesUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(
        userId: String,
        startDate: Long? = null,
        endDate: Long? = null,
        transportMode: String? = null,
        clientId: String? = null
    ): AppResult<List<TripExpense>> {
        Timber.d("📋 GetTripExpenses: Getting expenses for user: $userId")

        val result = repository.getExpenses(userId, startDate, endDate, transportMode, clientId)

        when (result) {
            is AppResult.Success -> Timber.d("✅ GetTripExpenses: Loaded ${result.data.size} expenses")
            is AppResult.Error -> Timber.e("❌ GetTripExpenses: Error - ${result.error.message}")
        }

        return result
    }
}

/**
 * Get a single expense by ID
 */
class GetExpenseByIdUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expenseId: String): AppResult<TripExpense> {
        Timber.d("📄 GetExpenseById: Getting expense: $expenseId")
        return repository.getExpenseById(expenseId)
    }
}

/**
 * Update an existing trip expense
 */
class UpdateTripExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: TripExpense): AppResult<TripExpense> {
        Timber.d("✏️ UpdateTripExpense: Updating expense: ${expense.id}")
        return repository.updateExpense(expense)
    }
}

/**
 * Delete a trip expense
 */
class DeleteTripExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expenseId: String): AppResult<Unit> {
        Timber.d("🗑️ DeleteTripExpense: Deleting expense: $expenseId")
        return repository.deleteExpense(expenseId)
    }
}

/**
 * Upload a receipt image
 */
class UploadReceiptUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(imageData: String, fileName: String): AppResult<String> {
        Timber.d("📸 UploadReceipt: Uploading receipt: $fileName")

        val result = repository.uploadReceipt(imageData, fileName)

        when (result) {
            is AppResult.Success -> Timber.d("✅ UploadReceipt: Uploaded to ${result.data}")
            is AppResult.Error -> Timber.e("❌ UploadReceipt: Error - ${result.error.message}")
        }

        return result
    }
}

/**
 * Get total amount spent by current user
 */
class GetTotalExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(): AppResult<Double> {
        Timber.d("💰 GetTotalExpense: Fetching total expenses")

        val result = repository.getTotalExpense()

        when (result) {
            is AppResult.Success -> Timber.d("✅ GetTotalExpense: Total = ${result.data}")
            is AppResult.Error -> Timber.e("❌ GetTotalExpense: Error - ${result.error.message}")
        }

        return result
    }
}