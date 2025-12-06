package com.bluemix.clients_lead.core.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Handles saving and retrieving auth token locally.
 * Token survives app restarts, process death, and removing from recents.
 */
class TokenStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        Log.d("TokenStorage", "💾 SAVING TOKEN: ${token.take(20)}...")
        prefs.edit()
            .putString("auth_token", token)
            .apply()

        // Verify it was saved
        val saved = prefs.getString("auth_token", null)
        Log.d("TokenStorage", "✅ TOKEN SAVED? ${saved != null} - Value: ${saved?.take(20)}")
    }

    fun getToken(): String? {
        val token = prefs.getString("auth_token", null)
        Log.d("TokenStorage", "🔍 GET TOKEN: ${if (token != null) token.take(20) + "..." else "NULL"}")
        return token
    }

    fun hasToken(): Boolean {
        val has = getToken() != null
        Log.d("TokenStorage", "❓ HAS TOKEN: $has")
        return has
    }

    fun clearToken() {
        Log.d("TokenStorage", "🗑️ CLEARING TOKEN")
        prefs.edit()
            .remove("auth_token")
            .apply()
    }
}