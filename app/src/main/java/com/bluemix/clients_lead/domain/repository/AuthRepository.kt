package com.bluemix.clients_lead.domain.repository

import com.bluemix.clients_lead.core.common.utils.AppResult
import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val id: String,
    val email: String,
    val token: String  // ✅ Add token here
)

data class AuthResponse(
    val token: String,
    val user: AuthUser
)

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AppResult<AuthResponse>  // ✅ Changed return type
    suspend fun signUp(email: String, password: String): AppResult<AuthResponse>  // ✅ Changed return type
    suspend fun signOut(): AppResult<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun currentUserId(): String?
    suspend fun sendMagicLink(email: String, redirectUrl: String? = null): AppResult<Unit>
    fun authState(): Flow<AuthUser?>
    suspend fun handleAuthRedirect(url: String): AppResult<AuthUser>  // ✅ Changed return type
}