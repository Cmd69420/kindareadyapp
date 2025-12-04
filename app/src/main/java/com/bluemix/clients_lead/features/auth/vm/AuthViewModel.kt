package com.bluemix.clients_lead.features.auth.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.usecases.HandleAuthRedirect
import com.bluemix.clients_lead.domain.usecases.IsLoggedIn
import com.bluemix.clients_lead.domain.usecases.ObserveAuthState
import com.bluemix.clients_lead.domain.usecases.SendMagicLink
import com.bluemix.clients_lead.domain.usecases.SignInWithEmail
import com.bluemix.clients_lead.domain.usecases.SignOut
import com.bluemix.clients_lead.domain.usecases.SignUpWithEmail
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

sealed interface AuthEffect {
    data object SignedIn : AuthEffect
}

class AuthViewModel(
    private val signUpWithEmail: SignUpWithEmail,
    private val signInWithEmail: SignInWithEmail,
    private val sendMagicLink: SendMagicLink,
    private val handleAuthRedirect: HandleAuthRedirect,
    private val isLoggedIn: IsLoggedIn,
    private val observeAuthState: ObserveAuthState,
    private val signOut: SignOut,
    private val authRedirectUrl: String,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        // react to auth changes (e.g., after deep link)
        viewModelScope.launch {
            observeAuthState().collect { user ->
                _state.value = _state.value.copy(email = user?.email ?: "")
                if (user != null) _effects.send(AuthEffect.SignedIn)
            }
        }
    }

    fun onEmailChange(v: String) {
        _state.value = _state.value.copy(email = v, error = null, info = null)
    }

    fun onPasswordChange(v: String) {
        _state.value = _state.value.copy(password = v, error = null, info = null)
    }

    fun doSignUp() = launch {
        val s = state.value
        if (!s.email.isValidEmail() || s.password.length < 6) {
            _state.value = s.copy(error = "Enter valid email and 6+ char password")
            return@launch
        }
        _state.value = s.copy(loading = true, error = null, info = null)
        when (val r = signUpWithEmail(s.email, s.password)) {
            is AppResult.Success -> _state.value = _state.value.copy(
                loading = false,
                info = "Verification link sent. Check your email."
            )

            is AppResult.Error -> _state.value = _state.value.copy(
                loading = false,
                error = r.error.message ?: "Sign up failed"
            )
        }
    }

    fun doSignIn() = launch {
        val s = state.value
        if (!s.email.isValidEmail() || s.password.isBlank()) {
            _state.value = s.copy(error = "Enter email and password")
            return@launch
        }
        _state.value = s.copy(loading = true, error = null, info = null)
        when (val r = signInWithEmail(s.email, s.password)) {
            is AppResult.Success -> {
                _state.value = _state.value.copy(loading = false)
                _effects.trySend(AuthEffect.SignedIn)
            }

            is AppResult.Error -> _state.value = _state.value.copy(
                loading = false,
                error = r.error.message ?: "Sign in failed"
            )
        }
    }

    fun sendMagicLink() = launch {
        val s = state.value
        if (!s.email.isValidEmail()) {
            _state.value = s.copy(error = "Enter valid email"); return@launch
        }
        _state.value = s.copy(loading = true, error = null, info = null)
        when (val r = sendMagicLink(s.email, authRedirectUrl)) {
            is AppResult.Success -> _state.value = _state.value.copy(
                loading = false,
                info = "Magic link sent. Check your email."
            )

            is AppResult.Error -> _state.value = _state.value.copy(
                loading = false,
                error = r.error.message ?: "Failed to send link"
            )
        }
    }

    private var isHandlingDeepLink = false

    fun handleDeepLink(uri: String) = viewModelScope.launch {
        if (isHandlingDeepLink) return@launch  // Prevent duplicate calls
        isHandlingDeepLink = true

        _state.value = _state.value.copy(loading = true, error = null, info = null)
        when (val r = handleAuthRedirect(uri)) {
            is AppResult.Success -> {
                _state.value = _state.value.copy(loading = false)
                _effects.trySend(AuthEffect.SignedIn)
            }

            is AppResult.Error -> _state.value = _state.value.copy(
                loading = false,
                error = r.error.message ?: "Invalid or expired link"
            )
        }

        isHandlingDeepLink = false
    }

//    /** Call this from your Activity when you receive the auth deep link. */
//    fun handleDeepLink(uri: String) = launch {
//        _state.value = _state.value.copy(loading = true, error = null, info = null)
//        when (val r = handleAuthRedirect(uri)) {
//            is AppResult.Success -> {
//                _state.value = _state.value.copy(loading = false)
//                _effects.trySend(AuthEffect.SignedIn)
//            }
//            is AppResult.Error -> _state.value = _state.value.copy(
//                loading = false,
//                error = r.error.message ?: "Invalid or expired link"
//            )
//        }
//    }

    fun doSignOut() = launch {
        _state.value = _state.value.copy(loading = true, error = null, info = null)
        when (val r = signOut()) {
            is AppResult.Success -> _state.value = _state.value.copy(
                loading = false,
                info = "Signed out"
            )

            is AppResult.Error -> _state.value = _state.value.copy(
                loading = false,
                error = r.error.message ?: "Sign out failed"
            )
        }
    }

    fun checkIfAlreadyLoggedIn() = launch {
        if (isLoggedIn()) _effects.trySend(AuthEffect.SignedIn)
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}

private fun String.isValidEmail(): Boolean =
    isNotBlank() && contains('@') && contains('.')
