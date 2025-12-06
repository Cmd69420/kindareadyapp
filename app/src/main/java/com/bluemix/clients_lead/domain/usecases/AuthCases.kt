package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.repository.AuthResponse
import com.bluemix.clients_lead.domain.repository.AuthUser
import kotlinx.coroutines.flow.Flow

// ==================== Sign In ====================
class SignInWithEmail(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<AuthResponse> =
        repo.signIn(email, password)
}

// ==================== Sign Up ====================
class SignUpWithEmail(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<AuthResponse> =
        repo.signUp(email, password)
}

// ==================== Sign Out ====================
class SignOut(private val repo: AuthRepository) {
    suspend operator fun invoke(): AppResult<Unit> =
        repo.signOut()
}

// ==================== Check Login Status ====================
class IsLoggedIn(private val repo: AuthRepository) {
    suspend operator fun invoke(): Boolean =
        repo.isLoggedIn()
}

// ==================== Get Current User ID ====================
class GetCurrentUserId(private val repo: AuthRepository) {
    suspend operator fun invoke(): String? =
        repo.currentUserId()
}

// ==================== Observe Auth State ====================
class ObserveAuthState(private val repo: AuthRepository) {
    operator fun invoke(): Flow<AuthUser?> =
        repo.authState()
}

// ==================== Magic Link ====================
class SendMagicLink(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, redirectUrl: String? = null): AppResult<Unit> =
        repo.sendMagicLink(email, redirectUrl)
}

// ==================== Handle Auth Redirect ====================
class HandleAuthRedirect(private val repo: AuthRepository) {
    suspend operator fun invoke(url: String): AppResult<AuthUser> =
        repo.handleAuthRedirect(url)
}