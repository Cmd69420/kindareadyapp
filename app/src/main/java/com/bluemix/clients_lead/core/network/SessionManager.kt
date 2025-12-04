package com.bluemix.clients_lead.core.network

import com.bluemix.clients_lead.domain.repository.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user session state across the app
 * Replaces Supabase's automatic session management
 *
 * This is the single source of truth for:
 * - Current user information
 * - Authentication state
 * - Session validity
 */
class SessionManager(
    private val tokenStorage: TokenStorage
) {

    // Internal mutable state
    private val _authState = MutableStateFlow<AuthUser?>(null)

    // Public immutable state - exposed to rest of app
    val authState: StateFlow<AuthUser?> = _authState.asStateFlow()

    /**
     * Set current authenticated user
     * Call this after successful login/signup
     */
    fun setUser(user: AuthUser) {
        _authState.value = user
    }

    /**
     * Get current user ID
     * Replaces: supabase.auth.currentUserOrNull()?.id
     */
    fun getCurrentUserId(): String? {
        return _authState.value?.id
    }

    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? {
        return _authState.value?.email
    }

    /**
     * Check if user is logged in
     * Replaces: supabase.auth.currentSessionOrNull() != null
     */
    fun isLoggedIn(): Boolean {
        return tokenStorage.hasToken() && _authState.value != null
    }

    /**
     * Clear session (on logout)
     */
    fun clearSession() {
        _authState.value = null
        tokenStorage.clearToken()
    }

    /**
     * Restore session from stored token
     * Call this on app startup to restore session
     */
    fun hasStoredSession(): Boolean {
        return tokenStorage.hasToken()
    }
}