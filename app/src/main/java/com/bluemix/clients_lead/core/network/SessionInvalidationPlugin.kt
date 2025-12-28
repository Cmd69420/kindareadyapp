package com.bluemix.clients_lead.core.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Ktor plugin that detects session invalidation (401 responses)
 * and triggers automatic logout.
 */
class SessionInvalidationPlugin(
    private val onSessionInvalidated: suspend () -> Unit
) {
    // ✅ Flag to prevent multiple logout triggers
    private var hasTriggeredLogout = false

    class Config {
        var onSessionInvalidated: (suspend () -> Unit)? = null
    }

    companion object : HttpClientPlugin<Config, SessionInvalidationPlugin> {
        override val key = AttributeKey<SessionInvalidationPlugin>("SessionInvalidationPlugin")

        override fun prepare(block: Config.() -> Unit): SessionInvalidationPlugin {
            val config = Config().apply(block)
            return SessionInvalidationPlugin(
                onSessionInvalidated = config.onSessionInvalidated
                    ?: error("onSessionInvalidated callback must be provided")
            )
        }

        override fun install(plugin: SessionInvalidationPlugin, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { request ->
                val call = execute(request)

                // Check if response is 401 Unauthorized
                if (call.response.status.value == 401) {
                    try {
                        val bodyText = call.response.bodyAsText()
                        val json = Json.parseToJsonElement(bodyText).jsonObject
                        val error = json["error"]?.jsonPrimitive?.content

                        // Check for session invalidation errors
                        if (error == "SESSION_INVALIDATED" ||
                            error == "TRIAL_EXPIRED" ||
                            error == "AccessTokenRequired") {

                            // ✅ Only trigger once
                            if (!plugin.hasTriggeredLogout) {
                                plugin.hasTriggeredLogout = true
                                Timber.w("🚨 Session invalidated! Error: $error")
                                plugin.onSessionInvalidated()
                            } else {
                                Timber.d("⏭️ Session already invalidated, skipping duplicate trigger")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing 401 response")
                    }
                }

                call
            }
        }
    }
}

/**
 * Extension function to easily add the plugin to HttpClient
 */
fun HttpClientConfig<*>.sessionInvalidation(
    onSessionInvalidated: suspend () -> Unit
) {
    install(SessionInvalidationPlugin) {
        this.onSessionInvalidated = onSessionInvalidated
    }
}