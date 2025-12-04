package com.bluemix.clients_lead.core.common.extensions

import com.bluemix.clients_lead.core.common.utils.AppError
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Error response format from your Node.js backend
 */
@Serializable
data class ApiErrorResponse(
    val error: String? = null,
    val message: String? = null
)

/**
 * Maps Ktor HTTP exceptions to app-specific errors
 * Replaces SupabaseErrorMapper
 */
suspend fun Throwable.toAppError(): AppError = when (this) {

    // Client errors (4xx)
    is ClientRequestException -> {
        val errorBody = try {
            val bodyText = response.bodyAsText()
            Json { ignoreUnknownKeys = true }.decodeFromString<ApiErrorResponse>(bodyText)
        } catch (e: Exception) {
            null
        }

        when (response.status.value) {
            400 -> AppError.Validation(
                message = errorBody?.error ?: errorBody?.message ?: "Invalid request",
                code = errorBody?.error,
                cause = this
            )

            401 -> AppError.Unauthorized(cause = this)

            403 -> AppError.Forbidden(cause = this)

            404 -> AppError.NotFound(cause = this)

            408 -> AppError.Network(message = "Request timeout", cause = this)

            409 -> AppError.Validation(
                message = errorBody?.message ?: "Conflict",
                code = "409",
                cause = this
            )

            422 -> AppError.Validation(
                message = errorBody?.message ?: "Validation failed",
                code = errorBody?.error,
                cause = this
            )

            429 -> {
                val retryAfter = response.headers["Retry-After"]?.toLongOrNull()
                AppError.RateLimited(
                    message = "Too many requests",
                    retryAfterSeconds = retryAfter,
                    cause = this
                )
            }

            else -> AppError.Unknown(cause = this)
        }
    }

    // Server errors (5xx)
    is ServerResponseException -> AppError.ServiceUnavailable(cause = this)

    // Timeout errors
    is HttpRequestTimeoutException -> AppError.Network(
        message = "Request timed out",
        cause = this
    )

    // Network errors
    is IOException -> AppError.Network(cause = this)

    // Cancellation - must be rethrown to properly cancel coroutines
    is CancellationException -> throw this

    // Unknown errors
    else -> AppError.Unknown(cause = this)
}