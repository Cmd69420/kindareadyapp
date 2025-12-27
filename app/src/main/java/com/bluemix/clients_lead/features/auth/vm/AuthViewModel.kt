package com.bluemix.clients_lead.features.auth.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.core.common.utils.AppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.common.utils.TrialManager
import com.bluemix.clients_lead.domain.usecases.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class AuthEffect {
    data object SignedIn : AuthEffect()
    data class ShowMessage(val message: String) : AuthEffect()
}

data class State(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val trialDaysRemaining: Long = 7,
    val isTrialExpired: Boolean = false,
    val canCreateAccount: Boolean = true
)

class AuthViewModel(
    private val signUpWithEmail: SignUpWithEmail,
    private val signInWithEmail: SignInWithEmail,
    private val sendMagicLink: SendMagicLink,
    private val handleAuthRedirect: HandleAuthRedirect,
    private val isLoggedIn: IsLoggedIn,
    private val signOut: SignOut,
    private val authRedirectUrl: String,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val trialManager = TrialManager(context)

    init {
        checkTrialStatus()
    }

    private fun checkTrialStatus() {
        val isValid = trialManager.isTrialValid()
        val daysRemaining = trialManager.getRemainingDays()
        val canCreate = trialManager.canCreateAccount()

        _state.update {
            it.copy(
                isTrialExpired = !isValid,
                trialDaysRemaining = daysRemaining,
                canCreateAccount = canCreate
            )
        }

        Timber.d("🕐 Trial Status: Valid=$isValid, Days=$daysRemaining, CanCreate=$canCreate")
    }

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun doSignUp() {
        // Trial validation
        if (_state.value.isTrialExpired) {
            _state.update { it.copy(error = "Trial period has ended. Please contact support.") }
            return
        }

        if (!trialManager.canCreateAccount()) {
            _state.update { it.copy(error = "Account creation limit reached on this device.") }
            return
        }

        val email = _state.value.email.trim()
        val password = _state.value.password

        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = signUpWithEmail(email, password)) {
                is AppResult.Success -> {
                    trialManager.recordAccountCreation()
                    checkTrialStatus()
                    _state.update { it.copy(loading = false) }
                    _effects.send(AuthEffect.SignedIn)
                }
                is AppResult.Error -> {
                    handleAuthError(result.error)
                }
            }
        }
    }

    fun doSignIn() {
        // Trial validation
        if (_state.value.isTrialExpired) {
            _state.update { it.copy(error = "Trial period has ended. Please contact support.") }
            return
        }

        val email = _state.value.email.trim()
        val password = _state.value.password

        if (!validateInput(email, password)) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = signInWithEmail(email, password)) {
                is AppResult.Success -> {
                    _state.update { it.copy(loading = false) }
                    _effects.send(AuthEffect.SignedIn)
                }
                is AppResult.Error -> {
                    handleAuthError(result.error)
                }
            }
        }
    }

    fun sendMagicLink() {
        val email = _state.value.email.trim()

        if (email.isBlank()) {
            _state.update { it.copy(error = "Please enter your email") }
            return
        }

        if (!isValidEmail(email)) {
            _state.update { it.copy(error = "Please enter a valid email") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            when (val result = sendMagicLink(email, authRedirectUrl)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            info = "Check your email for the magic link!"
                        )
                    }
                }
                is AppResult.Error -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            error = result.error.message ?: "Failed to send magic link"
                        )
                    }
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _state.update { it.copy(error = "Please enter your email") }
                false
            }
            !isValidEmail(email) -> {
                _state.update { it.copy(error = "Please enter a valid email") }
                false
            }
            password.isBlank() -> {
                _state.update { it.copy(error = "Please enter your password") }
                false
            }
            password.length < 6 -> {
                _state.update { it.copy(error = "Password must be at least 6 characters") }
                false
            }
            else -> true
        }
    }

    private fun handleAuthError(error: AppError) {
        val errorMessage = when (error) {
            is AppError.Unauthorized -> {
                if (error.message?.contains("TRIAL_EXPIRED") == true ||
                    error.message?.contains("expired") == true) {
                    trialManager.markTrialExpired()
                    checkTrialStatus()
                    "Your 7-day trial has ended. Please contact support."
                } else {
                    "Invalid email or password"
                }
            }
            is AppError.Validation -> error.message ?: "Validation error"
            is AppError.Network -> "Network error. Please check your connection."
            is AppError.Forbidden -> {
                if (error.message?.contains("TRIAL_EXPIRED") == true) {
                    trialManager.markTrialExpired()
                    checkTrialStatus()
                    "Your 7-day trial has ended. Please contact support."
                } else {
                    error.message ?: "Access denied"
                }
            }
            else -> error.message ?: "Authentication failed"
        }

        _state.update { it.copy(loading = false, error = errorMessage) }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}