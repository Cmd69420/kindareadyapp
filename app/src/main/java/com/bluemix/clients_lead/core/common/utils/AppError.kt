package com.bluemix.clients_lead.core.common.utils

sealed class AppError(
    open val message: String? = null,
    open val cause: Throwable? = null
) {
    data class Network(
        override val message: String? = "Network error",
        val code: Int? = null,
        val isRetryable: Boolean = true,  // ADD THIS
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Unauthorized(
        override val message: String? = "Unauthorized",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Forbidden(
        override val message: String? = "Forbidden",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class NotFound(
        override val message: String? = "Not found",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    // core/common/AppError.kt
    data class Validation(
        val fieldErrors: Map<String, String> = emptyMap(),
        override val message: String? = "Validation failed",
        val code: String? = null,                // <— add this
        override val cause: Throwable? = null
    ) : AppError(message, cause)


    data class RateLimited(
        override val message: String? = "Too many requests",
        val retryAfterSeconds: Long? = null,
        val isRetryable: Boolean = true,  // ADD THIS
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class ServiceUnavailable(
        override val message: String? = "Service unavailable",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message ?: cause?.message, cause)
}
