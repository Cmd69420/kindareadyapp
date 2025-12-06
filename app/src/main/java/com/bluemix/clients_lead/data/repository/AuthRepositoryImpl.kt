package com.bluemix.clients_lead.data.repository

import android.util.Log
import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.core.network.SessionManager
import com.bluemix.clients_lead.core.network.TokenStorage
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.repository.AuthResponse
import com.bluemix.clients_lead.domain.repository.AuthUser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): AppResult<AuthResponse> =
        runAppCatching(mapper = { it.toAppError() }) {
            Log.d("AUTH", "Attempting login to: ${ApiEndpoints.BASE_URL}${ApiEndpoints.Auth.LOGIN}")

            val response = httpClient.post(ApiEndpoints.Auth.LOGIN) {
                setBody(LoginRequest(email = email, password = password))
            }.body<LoginResponse>()

            Log.d("AUTH", "Login successful: ${response.token}")

            // Save token immediately
            tokenStorage.saveToken(response.token)

            val authUser = AuthUser(
                id = response.user.id,
                email = response.user.email,
                token = response.token
            )

            // Update session with user info
            sessionManager.setUser(authUser)

            // Return the auth response
            AuthResponse(token = response.token, user = authUser)
        }

    override suspend fun signUp(email: String, password: String): AppResult<AuthResponse> =
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.post(ApiEndpoints.Auth.SIGNUP) {
                setBody(SignupRequest(email = email, password = password))
            }.body<SignupResponse>()

            // Save token immediately
            tokenStorage.saveToken(response.token)

            val authUser = AuthUser(
                id = response.user.id,
                email = response.user.email,
                token = response.token
            )

            // Update session with user info
            sessionManager.setUser(authUser)

            // Return the auth response
            AuthResponse(token = response.token, user = authUser)
        }

    override suspend fun signOut(): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            // Clear session and token
            sessionManager.clearSession()
            // Token will expire after 7 days on backend
        }

    override suspend fun isLoggedIn(): Boolean =
        sessionManager.isLoggedIn()

    override suspend fun currentUserId(): String? {
        // If we have user in memory, return it
        val cachedUserId = sessionManager.getCurrentUserId()
        if (cachedUserId != null) {
            return cachedUserId
        }

        // If we have a token but no user in memory (app restart case)
        // Fetch profile to restore session
        if (tokenStorage.hasToken()) {
            try {
                Log.d("AUTH", "📥 Restoring user session from token...")
                val response = httpClient.get(ApiEndpoints.Auth.PROFILE).body<ProfileResponse>()

                val token = tokenStorage.getToken()!!
                val authUser = AuthUser(
                    id = response.user.id,
                    email = response.user.email,
                    token = token
                )

                sessionManager.setUser(authUser)
                Log.d("AUTH", "✅ Session restored for user: ${response.user.email}")
                return response.user.id

            } catch (e: Exception) {
                Log.e("AUTH", "❌ Failed to restore session, clearing token", e)
                // Token is invalid or expired, clear it
                sessionManager.clearSession()
                return null
            }
        }

        return null
    }

    override suspend fun sendMagicLink(email: String, redirectUrl: String?): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            throw NotImplementedError("Magic link not implemented in backend")
        }

    override fun authState(): Flow<AuthUser?> =
        sessionManager.authState

    override suspend fun handleAuthRedirect(redirectUrl: String): AppResult<AuthUser> =
        runAppCatching(mapper = { it.toAppError() }) {
            throw NotImplementedError("Auth redirect not implemented in backend")
        }
}

// ==================== Request/Response Models ====================

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null
)

@Serializable
data class LoginResponse(
    val message: String,
    val token: String,
    val user: UserData
)

@Serializable
data class SignupResponse(
    val message: String,
    val token: String,
    val user: UserDataWithProfile
)

@Serializable
data class UserData(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null
)

@Serializable
data class UserDataWithProfile(
    val id: String,
    val email: String,
    val profile: ProfileData? = null,
    val createdAt: String? = null
)

@Serializable
data class ProfileData(
    val id: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null
)

@Serializable
data class ProfileResponse(
    val user: UserData
)