package com.bluemix.clients_lead.data.repository

import android.net.Uri
import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.core.network.SessionManager
import com.bluemix.clients_lead.core.network.TokenStorage
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.repository.AuthUser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import android.util.Log

/**
 * Updated AuthRepository using REST API instead of Supabase
 */
class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            try {
                Log.d("AUTH", "Attempting login to: ${ApiEndpoints.BASE_URL}${ApiEndpoints.Auth.LOGIN}")

                val response = httpClient.post(ApiEndpoints.Auth.LOGIN) {
                    setBody(LoginRequest(email = email, password = password))
                }.body<LoginResponse>()

                Log.d("AUTH", "Login successful: ${response.token}")

                tokenStorage.saveToken(response.token)
                sessionManager.setUser(AuthUser(id = response.user.id, email = response.user.email))
            } catch (e: Exception) {
                Log.e("AUTH", "Login failed: ${e.message}", e)
                throw e
            }
        }

    override suspend fun signUp(email: String, password: String): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            val response = httpClient.post(ApiEndpoints.Auth.SIGNUP) {
                setBody(SignupRequest(email = email, password = password))
            }.body<SignupResponse>()

            // Save token
            tokenStorage.saveToken(response.token)

            // Update session
            sessionManager.setUser(
                AuthUser(
                    id = response.user.id,
                    email = response.user.email
                )
            )
        }

    override suspend fun signOut(): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            // Clear session and token
            sessionManager.clearSession()

            // Note: Your backend doesn't have a logout endpoint
            // Token will just expire after 7 days
            // If you add logout endpoint later, call it here
        }

    override suspend fun isLoggedIn(): Boolean =
        sessionManager.isLoggedIn()

    override suspend fun currentUserId(): String? {
        // If we have token but no user in session, fetch user profile
        if (tokenStorage.hasToken() && sessionManager.getCurrentUserId() == null) {
            try {
                val response = httpClient.get(ApiEndpoints.Auth.PROFILE)
                    .body<ProfileResponse>()

                sessionManager.setUser(
                    AuthUser(
                        id = response.user.id,
                        email = response.user.email
                    )
                )
                return response.user.id
            } catch (e: Exception) {
                // Token might be invalid, clear it
                sessionManager.clearSession()
                return null
            }
        }

        return sessionManager.getCurrentUserId()
    }

    override suspend fun sendMagicLink(email: String, redirectUrl: String?): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            // Your backend doesn't have magic link endpoint yet
            // You can implement this later if needed
            throw NotImplementedError("Magic link not implemented in backend")
        }

    override fun authState(): Flow<AuthUser?> =
        sessionManager.authState

    override suspend fun handleAuthRedirect(redirectUrl: String): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            // Your backend doesn't use OAuth/magic links yet
            // You can implement this later if needed
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