package com.bluemix.clients_lead.domain.usecases

import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.repository.AuthUser
import kotlinx.coroutines.flow.Flow

class SignUpWithEmail(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<Unit> =
        repo.signUp(email, password)
}

class SignInWithEmail(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<Unit> =
        repo.signIn(email, password)
}

class SendMagicLink(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, redirectUrl: String?): AppResult<Unit> =
        repo.sendMagicLink(email, redirectUrl)
}

/** Use string so domain stays platform-agnostic (UI can pass Uri.toString()). */
class HandleAuthRedirect(private val repo: AuthRepository) {
    suspend operator fun invoke(deepLink: String): AppResult<Unit> =
        repo.handleAuthRedirect(deepLink)
}

class IsLoggedIn(private val repo: AuthRepository) {
    suspend operator fun invoke(): Boolean = repo.isLoggedIn()
}

class SignOut(private val repo: AuthRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repo.signOut()
}

class CurrentUserId(private val repo: AuthRepository) {
    suspend operator fun invoke(): String? = repo.currentUserId()
}

class ObserveAuthState(private val repo: AuthRepository) {
     operator fun invoke(): Flow<AuthUser?> = repo.authState()
}
class GetCurrentUserId(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): String? = repository.currentUserId()
}