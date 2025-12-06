package com.bluemix.clients_lead.core.network

import android.util.Log
import com.bluemix.clients_lead.domain.repository.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user session state across the app.
 *
 * This is the single source of truth for:
 * - Current user information (in-memory)
 * - Authentication state
 * - Session validity (via stored token)
 */
class SessionManager(
    private val tokenStorage: TokenStorage
) {

    // Internal mutable state
    private val _authState = MutableStateFlow<AuthUser?>(null)

    // Public immutable state - exposed to rest of app
    val authState: StateFlow<AuthUser?> = _authState.asStateFlow()

    /**
     * Set current authenticated user.
     * Call this after successful login/signup.
     */
    fun setUser(user: AuthUser) {
        Log.d("SessionManager", "👤 SET USER: ${user.email} (id: ${user.id})")
        _authState.value = user
    }

    /**
     * Get current user ID.
     */
    fun getCurrentUserId(): String? {
        val userId = _authState.value?.id
        Log.d("SessionManager", "🆔 GET USER ID: $userId")
        return userId
    }

    /**
     * Get current user email.
     */
    fun getCurrentUserEmail(): String? {
        val email = _authState.value?.email
        Log.d("SessionManager", "📧 GET USER EMAIL: $email")
        return email
    }

    /**
     * Check if user is logged in.
     *
     * IMPORTANT:
     * - After process death, _authState will be null but the token
     *   in SharedPreferences is still valid.
     * - For gating/navigation we only care if a token exists.
     * - Backend will validate token on each request and return 401 if invalid.
     */
    fun isLoggedIn(): Boolean {
        val hasToken = tokenStorage.hasToken()
        Log.d("SessionManager", "🔐 IS LOGGED IN: $hasToken")
        return hasToken
    }

    /**
     * Clear session (on logout).
     */
    fun clearSession() {
        Log.d("SessionManager", "🚪 CLEARING SESSION")
        _authState.value = null
        tokenStorage.clearToken()
    }

    /**
     * Check if we have a stored token (same as isLoggedIn currently).
     * Kept for clarity / future extension.
     */
    fun hasStoredSession(): Boolean {
        val hasToken = tokenStorage.hasToken()
        Log.d("SessionManager", "💾 HAS STORED SESSION: $hasToken")
        return hasToken
    }
}