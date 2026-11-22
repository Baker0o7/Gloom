package dev.materii.gloom.api.util

import dev.materii.gloom.util.Logger
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging

fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installLogging(loggerDI: Logger) {
    install(Logging) {
        // Method, URL and status only: bodies contain private repo/user data, and the
        // Authorization header must never reach logcat.
        level = LogLevel.INFO
        sanitizeHeader { header -> header.equals("Authorization", ignoreCase = true) }
        logger = object: io.ktor.client.plugins.logging.Logger {
            override fun log(message: String) {
                loggerDI.debug("HTTP", message)
            }
        }
    }
}