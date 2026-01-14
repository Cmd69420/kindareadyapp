package com.bluemix.clients_lead.data.repository

import android.content.Context
import android.util.Log
import com.bluemix.clients_lead.core.common.extensions.runAppCatching
import com.bluemix.clients_lead.core.common.extensions.toAppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.core.common.utils.DeviceIdentifier
import com.bluemix.clients_lead.core.network.ApiEndpoints
import com.bluemix.clients_lead.core.network.SessionManager
import com.bluemix.clients_lead.core.network.TokenStorage
import com.bluemix.clients_lead.domain.repository.AuthRepository
import com.bluemix.clients_lead.domain.repository.AuthResponse
import com.bluemix.clients_lead.domain.repository.AuthUser
import com.bluemix.clients_lead.core.common.utils.TrialManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val context: Context
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): AppResult<AuthResponse> =
        runAppCatching(mapper = { it.toAppError() }) {
            Log.d("AUTH", "Attempting login to: ${ApiEndpoints.BASE_URL}${ApiEndpoints.Auth.LOGIN}")

            val deviceId = DeviceIdentifier.getDeviceId(context)

            val response = httpClient.post(ApiEndpoints.Auth.LOGIN) {
                setBody(LoginRequest(
                    email = email,
                    password = password,
                    deviceId = deviceId
                ))
            }.body<LoginResponse>()

            Log.d("AUTH", "Login successful: ${response.token}")

            // Save token immediately
            tokenStorage.saveToken(response.token)

            // ✅ Parse isTrialUser from backend response
            val authUser = AuthUser(
                id = response.user.id,
                email = response.user.email,
                token = response.token,
                isTrialUser = response.user.isTrialUser ?: false,  // ✅ NEW
                companyId = response.user.companyId,                // ✅ NEW
                companyName = response.user.companyName             // ✅ NEW
            )
            // ✅ CLEAR DEVICE TRIAL STATE IF COMPANY USER (AUTO-LOGIN FIX)
            TrialManager(context).clearTrialIfCompanyUser(authUser.isTrialUser)


            // Update session with user info
            sessionManager.setUser(authUser)

            Log.d("AUTH", "✅ User logged in - Trial: ${authUser.isTrialUser}, Company: ${authUser.companyName}")

            // Return the auth response
            AuthResponse(token = response.token, user = authUser)
        }

    override suspend fun signUp(email: String, password: String): AppResult<AuthResponse> =
        runAppCatching(mapper = { it.toAppError() }) {
            val deviceId = DeviceIdentifier.getDeviceId(context)

            val response = httpClient.post(ApiEndpoints.Auth.SIGNUP) {
                setBody(SignupRequest(
                    email = email,
                    password = password,
                    deviceId = deviceId
                ))
            }.body<SignupResponse>()

            // Save token immediately
            tokenStorage.saveToken(response.token)

            // ✅ Parse isTrialUser from backend response
            val authUser = AuthUser(
                id = response.user.id,
                email = response.user.email,
                token = response.token,
                isTrialUser = response.user.isTrialUser ?: true,   // ✅ Default to trial if not specified
                companyId = response.user.companyId,                // ✅ NEW
                companyName = response.user.companyName             // ✅ NEW
            )

            // Update session with user info
            sessionManager.setUser(authUser)

            Log.d("AUTH", "✅ User signed up - Trial: ${authUser.isTrialUser}, Company: ${authUser.companyName}")

            // Return the auth response
            AuthResponse(token = response.token, user = authUser)
        }

    override suspend fun signOut(): AppResult<Unit> =
        runAppCatching(mapper = { it.toAppError() }) {
            // Clear session and token
            sessionManager.clearSession()
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
                Log.d("AUTH", "🔥 Restoring user session from token...")
                val response = httpClient.get(ApiEndpoints.Auth.PROFILE).body<ProfileResponse>()

                val token = tokenStorage.getToken()!!

                // ✅ Restore full user data including trial status
                val authUser = AuthUser(
                    id = response.user.id,
                    email = response.user.email,
                    token = token,
                    isTrialUser = response.user.isTrialUser ?: false,  // ✅ NEW
                    companyId = response.user.companyId,                // ✅ NEW
                    companyName = response.user.companyName             // ✅ NEW
                )

                sessionManager.setUser(authUser)
                Log.d("AUTH", "✅ Session restored for user: ${response.user.email} (Trial: ${authUser.isTrialUser})")
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
    val password: String,
    val deviceId: String? = null
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null,
    val deviceId: String? = null
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
    val user: UserDataWithProfile,
    val signupType: String? = null  // "trial" or "company"
)

@Serializable
data class UserData(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val department: String? = null,
    val workHoursStart: String? = null,
    val workHoursEnd: String? = null,
    val isTrialUser: Boolean? = null,      // ✅ NEW
    val companyId: String? = null,          // ✅ NEW
    val companyName: String? = null         // ✅ NEW
)

@Serializable
data class UserDataWithProfile(
    val id: String,
    val email: String,
    val profile: ProfileData? = null,
    val createdAt: String? = null,
    val isTrialUser: Boolean? = null,      // ✅ NEW
    val companyId: String? = null,          // ✅ NEW
    val companyName: String? = null         // ✅ NEW
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