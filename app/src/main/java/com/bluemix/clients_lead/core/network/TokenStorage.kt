package com.bluemix.clients_lead.core.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Handles storage and retrieval of JWT authentication tokens
 * Replaces Supabase's automatic token management
 */
class TokenStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save authentication token
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Get stored authentication token
     * @return Token if exists, null otherwise
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Check if valid token exists
     */
    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    /**
     * Clear stored token (on logout)
     */
    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    /**
     * Clear all auth data
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }
}