package dev.materii.gloom.api.service.ai

import dev.materii.gloom.api.dto.ai.ChatCompletionRequest
import dev.materii.gloom.api.dto.ai.ChatCompletionResponse
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.util.ApiError
import dev.materii.gloom.api.util.ApiFailure
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import dev.materii.gloom.domain.manager.PreferenceManager
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI Service backed by the z.ai API (OpenAI-compatible).
 *
 * Default endpoint: https://api.z.ai/v1/chat/completions
 * Users can override the base URL in Settings → AI Settings.
 * An API key from z.ai is required (https://z.ai).
 */
class AIService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val authManager: AuthManager,
    private val prefs: PreferenceManager,
) {

    companion object {
        // Z.AI endpoint — https://docs.z.ai/guides/overview/quick-start
        const val DEFAULT_BASE_URL = "https://api.z.ai/api/paas/v4"
        // glm-4-flash is the free fast tier per Z.AI docs
        const val DEFAULT_MODEL    = "glm-4-flash"

        val AVAILABLE_MODELS = listOf(
            ModelInfo("glm-4-flash", "GLM-4 Flash", "Z.AI", "Free tier — fast responses"),
            ModelInfo("glm-4.5",     "GLM-4.5",     "Z.AI", "Balanced performance"),
            ModelInfo("glm-4.7",     "GLM-4.7",     "Z.AI", "Advanced reasoning & coding"),
            ModelInfo("glm-5",       "GLM-5",       "Z.AI", "Flagship — most capable"),
        )
    }

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val publisher: String,
        val description: String,
    )

    fun isEnabled(): Boolean = prefs.aiEnabled

    /** Active base URL — user custom URL or the z.ai default. */
    private fun baseUrl(): String =
        prefs.aiApiUrl.trim().ifBlank { DEFAULT_BASE_URL }.trimEnd('/')

    /** Active API key. */
    private fun apiKey(): String = prefs.aiApiKey.trim()

    fun hasApiKey(): Boolean = apiKey().isNotBlank()

    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 4096,
    ): ApiResponse<ChatCompletionResponse> {
        val key = apiKey()
        if (key.isBlank()) {
            return ApiResponse.Error(
                ApiError(HttpStatusCode.Unauthorized, "No Z.AI API key configured. Add one in Settings → AI Settings.")
            )
        }

        return try {
            val requestBody = ChatCompletionRequest(
                messages    = messages,
                model       = model,
                temperature = temperature,
                maxTokens   = maxTokens,
            )

            val response = httpClient.post("${baseUrl()}/chat/completions") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $key")
                setBody(json.encodeToString(requestBody))
            }

            when {
                response.status.isSuccess() -> {
                    val body   = response.bodyAsText()
                    val parsed = json.decodeFromString<ChatCompletionResponse>(body)
                    if (parsed.choices.isNotEmpty() &&
                        parsed.choices[0].message.content.isNotBlank()) {
                        ApiResponse.Success(parsed)
                    } else {
                        ApiResponse.Error(ApiError(response.status, "Empty response from Z.AI"))
                    }
                }
                response.status == HttpStatusCode.Unauthorized ->
                    ApiResponse.Error(ApiError(response.status, "Invalid API key. Check Settings → AI Settings."))
                else ->
                    ApiResponse.Error(ApiError(response.status, "Z.AI error: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResponse.Failure(ApiFailure(e, e.message))
        }
    }

    fun getAvailableModels(): List<ModelInfo> = AVAILABLE_MODELS

    fun createCodingSystemMessage() = ChatMessage(
        role    = "system",
        content = """
            You are an expert coding assistant integrated into Gloom, a GitHub client app.
            You help users with:
            - Understanding code and repositories
            - Writing and debugging code (prefer Kotlin for Android examples)
            - Explaining programming concepts
            - Answering questions about GitHub features
            - Code review and best practices

            Format code blocks with appropriate language tags (```kotlin, ```xml, etc.).
            Be concise, friendly, and professional.
        """.trimIndent()
    )

    fun createUserMessage(content: String)      = ChatMessage(role = "user",      content = content)
    fun createAssistantMessage(content: String) = ChatMessage(role = "assistant", content = content)
}
