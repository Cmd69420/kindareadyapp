package com.bluemix.clients_lead.features.auth.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.domain.repository.AuthUser
import com.bluemix.clients_lead.domain.usecases.IsLoggedIn
import com.bluemix.clients_lead.domain.usecases.ObserveAuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionState(
    val isReady: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: AuthUser? = null
)

class SessionViewModel(
    private val isLoggedIn: IsLoggedIn,
    private val observeAuthState: ObserveAuthState
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    init {
        Log.d("SessionViewModel", "🚀 INIT - Starting session check")
        checkInitialAuthState()
        observeLiveAuthChanges()
    }

    private fun checkInitialAuthState() {
        viewModelScope.launch {
            // Check if token exists on device (survives app restart)
            val initialAuthed = runCatching {
                isLoggedIn()
            }.getOrElse {
                Log.e("SessionViewModel", "❌ Error checking login status", it)
                false
            }

            Log.d("SessionViewModel", "🔍 Initial auth status: $initialAuthed")

            _state.update {
                it.copy(
                    isReady = true,
                    isAuthenticated = initialAuthed
                )
            }

            Log.d("SessionViewModel", "✅ Session ready - isAuthenticated: $initialAuthed")
        }
    }

    private fun observeLiveAuthChanges() {
        viewModelScope.launch {
            // Only listen for active login/logout events
            observeAuthState().collect { user ->
                Log.d("SessionViewModel", "👤 Auth state changed: ${user?.email ?: "null"}")

                // CRITICAL FIX: Only update if user is not null (actual login event)
                // Don't override with null on app restart
                if (user != null) {
                    Log.d("SessionViewModel", "✅ User logged in: ${user.email}")
                    _state.update { it.copy(isAuthenticated = true, user = user) }
                } else {
                    // User explicitly logged out (SessionManager.clearSession was called)
                    // Only clear if we don't have a token anymore
                    val hasToken = runCatching { isLoggedIn() }.getOrDefault(false)
                    if (!hasToken) {
                        Log.d("SessionViewModel", "🚪 User logged out")
                        _state.update { it.copy(isAuthenticated = false, user = null) }
                    }
                }
            }
        }
    }
}