package com.bluemix.clients_lead.core.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClientProvider {

    fun create(
        baseUrl: String,
        tokenStorage: TokenStorage,
        sessionManager: SessionManager // ✅ Added SessionManager parameter
    ): HttpClient = HttpClient(OkHttp) {

        // Base URL configuration
        defaultRequest {
            url(baseUrl)
            header("Content-Type", "application/json")

            // Add Bearer token to every request (loaded fresh each time)
            val token = tokenStorage.getToken()
            if (token != null) {
                header("Authorization", "Bearer $token")
            }
        }

        // JSON serialization
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                prettyPrint = false
                coerceInputValues = true
            })
        }

        // ✅ NEW: Session invalidation detection
        sessionInvalidation {
            // This callback runs when a 401 with SESSION_INVALIDATED is detected
            sessionManager.clearSession(wasInvalidated = true)
        }

        // Logging (set to NONE in production)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }

        // Timeout configuration
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }

        // Validate responses (throws exception on non-2xx status codes)
        expectSuccess = true
    }
}