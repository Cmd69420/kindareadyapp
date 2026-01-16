// data/repository/DraftExpenseRepository.kt
package com.bluemix.clients_lead.data.repository

import android.content.Context
import com.bluemix.clients_lead.domain.model.DraftExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

/**
 * Local storage repository for draft expenses
 * Saves drafts to internal storage as JSON files
 */
class DraftExpenseRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val draftsDir = File(context.filesDir, "expense_drafts").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Save a draft expense
     */
    suspend fun saveDraft(draft: DraftExpense): Result<DraftExpense> = withContext(Dispatchers.IO) {
        try {
            val updatedDraft = draft.copy(lastModified = System.currentTimeMillis())
            val file = File(draftsDir, "${draft.id}.json")
            val jsonString = json.encodeToString(updatedDraft)

            file.writeText(jsonString)

            Timber.d("💾 Draft saved: ${draft.id}")
            Result.success(updatedDraft)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save draft")
            Result.failure(e)
        }
    }

    /**
     * Get all drafts for a user
     */
    suspend fun getDrafts(userId: String): Result<List<DraftExpense>> = withContext(Dispatchers.IO) {
        try {
            val drafts = draftsDir.listFiles()
                ?.mapNotNull { file ->
                    try {
                        val jsonString = file.readText()
                        json.decodeFromString<DraftExpense>(jsonString)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse draft: ${file.name}")
                        null
                    }
                }
                ?.filter { it.userId == userId }
                ?.sortedByDescending { it.lastModified }
                ?: emptyList()

            Timber.d("📋 Loaded ${drafts.size} drafts for user: $userId")
            Result.success(drafts)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load drafts")
            Result.failure(e)
        }
    }

    /**
     * Get a specific draft by ID
     */
    suspend fun getDraftById(draftId: String): Result<DraftExpense?> = withContext(Dispatchers.IO) {
        try {
            val file = File(draftsDir, "$draftId.json")

            if (!file.exists()) {
                return@withContext Result.success(null)
            }

            val jsonString = file.readText()
            val draft = json.decodeFromString<DraftExpense>(jsonString)

            Timber.d("📄 Loaded draft: $draftId")
            Result.success(draft)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load draft: $draftId")
            Result.failure(e)
        }
    }

    /**
     * Delete a draft
     */
    suspend fun deleteDraft(draftId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(draftsDir, "$draftId.json")

            if (file.exists()) {
                file.delete()
                Timber.d("🗑️ Deleted draft: $draftId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete draft: $draftId")
            Result.failure(e)
        }
    }

    /**
     * Clear all drafts for a user
     */
    suspend fun clearAllDrafts(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deletedCount = draftsDir.listFiles()
                ?.mapNotNull { file ->
                    try {
                        val jsonString = file.readText()
                        val draft = json.decodeFromString<DraftExpense>(jsonString)
                        if (draft.userId == userId) {
                            file.delete()
                            1
                        } else 0
                    } catch (e: Exception) {
                        0
                    }
                }
                ?.sum() ?: 0

            Timber.d("🧹 Cleared $deletedCount drafts for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear drafts")
            Result.failure(e)
        }
    }

    /**
     * Get count of drafts for a user
     */
    suspend fun getDraftCount(userId: String): Int = withContext(Dispatchers.IO) {
        draftsDir.listFiles()
            ?.count { file ->
                try {
                    val jsonString = file.readText()
                    val draft = json.decodeFromString<DraftExpense>(jsonString)
                    draft.userId == userId
                } catch (e: Exception) {
                    false
                }
            } ?: 0
    }
}