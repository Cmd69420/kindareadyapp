package com.bluemix.clients_lead.features.auth.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.domain.repository.AuthUser
import com.bluemix.clients_lead.domain.usecases.IsLoggedIn
import com.bluemix.clients_lead.domain.usecases.ObserveAuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds global auth/session state for navigation gating.
 *
 * Improvements:
 * - Uses stateIn() for proper lifecycle management
 * - Prevents memory leaks from unconstrained Flow collection
 * - Handles session changes gracefully with 5-second timeout
 */
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

    // Convert cold Flow to hot StateFlow with lifecycle awareness
    private val authStateFlow = observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep alive 5s after last subscriber
            initialValue = null
        )

    init {
        viewModelScope.launch {
            // 1) Check cached login status (fast path)
            val initialAuthed = runCatching {
                isLoggedIn()
            }.getOrDefault(false)

            _state.update {
                it.copy(
                    isReady = true,
                    isAuthenticated = initialAuthed
                )
            }

            // 2) Subscribe to live auth state changes
            authStateFlow.collect { user ->
                _state.update { s ->
                    s.copy(
                        isAuthenticated = (user != null),
                        user = user
                    )
                }
            }
        }
    }
}