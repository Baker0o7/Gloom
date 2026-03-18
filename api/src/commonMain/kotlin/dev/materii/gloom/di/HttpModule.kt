package dev.materii.gloom.di

import dev.materii.gloom.api.util.installLogging
import dev.materii.gloom.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun httpModule() = module {

    fun provideAuthClient(
        json: Json,
        logger: Logger
    ): HttpClient {
        return HttpClient(CIO) {
            defaultRequest {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.UserAgent, UserAgent)
                header(HttpHeaders.AcceptLanguage, "en-US")
            }
            install(HttpRequestRetry) {
                maxRetries = 5
                retryIf { _, httpResponse ->
                    // Don't retry 4xx client errors (401, 403, 404, etc.)
                    val status = httpResponse.status
                    !status.isSuccess() && status.value !in 400..499
                }
                retryOnExceptionIf { _, error ->
                    error is HttpRequestTimeoutException
                }
                delayMillis { retry ->
                    retry * 1000L
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            installLogging(logger)
        }
    }

    fun provideRestClient(
        json: Json,
        logger: Logger
    ): HttpClient {
        return HttpClient(CIO) {
            defaultRequest {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.UserAgent, UserAgent)
                header(HttpHeaders.AcceptLanguage, "en-US")
            }
            install(HttpRequestRetry) {
                maxRetries = 5
                retryIf { _, httpResponse ->
                    // Don't retry 4xx client errors (401, 403, 404, etc.)
                    val status = httpResponse.status
                    !status.isSuccess() && status.value !in 400..499
                }
                retryOnExceptionIf { _, error ->
                    error is HttpRequestTimeoutException
                }
                delayMillis { retry ->
                    retry * 1000L
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            installLogging(logger)
        }
    }

    fun provideAIClient(
        json: Json,
        logger: Logger
    ): HttpClient {
        return HttpClient(CIO) {
            defaultRequest {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { _, httpResponse ->
                    val status = httpResponse.status
                    !status.isSuccess() && status.value !in 400..499
                }
                retryOnExceptionIf { _, error ->
                    error is HttpRequestTimeoutException
                }
                delayMillis { retry ->
                    retry * 1000L
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            installLogging(logger)
        }
    }

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

    single(named("AI")) {
        provideAIClient(get(), get())
    }

}