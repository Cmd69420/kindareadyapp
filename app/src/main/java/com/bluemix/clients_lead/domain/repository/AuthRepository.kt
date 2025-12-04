package com.bluemix.clients_lead.domain.repository


import com.bluemix.clients_lead.core.common.utils.AppResult
import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val id: String,
    val email: String?
)

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AppResult<Unit>
    suspend fun signUp(email: String, password: String): AppResult<Unit>
    suspend fun signOut(): AppResult<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun currentUserId(): String?
    suspend fun sendMagicLink(email: String, redirectUrl: String? = null): AppResult<Unit>
    fun authState(): Flow<AuthUser?>
    suspend fun handleAuthRedirect(url: String): AppResult<Unit>

}
