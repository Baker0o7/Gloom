package dev.materii.gloom.di

import dev.materii.gloom.api.util.installLogging
import dev.materii.gloom.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpMethod
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Shared client setup. Timeouts stop a stalled connection from leaving a screen spinning forever,
 * and retries are limited to idempotent GET/HEAD requests so a retried POST (create gist, re-run
 * workflow, star...) can never be applied twice.
 */
private fun HttpClientConfig<*>.configureClient(json: Json, logger: Logger) {
    defaultRequest {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        header(HttpHeaders.UserAgent, UserAgent)
        header(HttpHeaders.AcceptLanguage, "en-US")
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        socketTimeoutMillis  = 30_000
        requestTimeoutMillis = 60_000
    }
    install(HttpRequestRetry) {
        maxRetries = 3
        retryIf { request, httpResponse ->
            val idempotent = request.method == HttpMethod.Get || request.method == HttpMethod.Head
            val status = httpResponse.status
            // Never retry 4xx (401, 403, 404, 422...): they won't change by asking again.
            idempotent && !status.isSuccess() && status.value !in 400..499
        }
        retryOnExceptionIf { request, error ->
            val idempotent = request.method == HttpMethod.Get || request.method == HttpMethod.Head
            idempotent && (
                error is HttpRequestTimeoutException ||
                    error is ConnectTimeoutException ||
                    error is SocketTimeoutException
                )
        }
        delayMillis { retry -> retry * 1000L }
    }
    install(ContentNegotiation) {
        json(json)
    }
    installLogging(logger)
}

fun httpModule() = module {

    fun provideAuthClient(json: Json, logger: Logger): HttpClient =
        HttpClient(CIO) { configureClient(json, logger) }

    fun provideRestClient(json: Json, logger: Logger): HttpClient =
        HttpClient(CIO) { configureClient(json, logger) }

    single {
        Json {
            ignoreUnknownKeys = true
        }
    }

    single(named("Auth")) {
        provideAuthClient(get(), get())
    }

    single(named("Rest")) {
        provideRestClient(get(), get())
    }

}
