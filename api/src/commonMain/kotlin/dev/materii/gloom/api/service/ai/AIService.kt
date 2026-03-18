package dev.materii.gloom.api.service.ai

import dev.materii.gloom.api.dto.ai.*
import dev.materii.gloom.api.util.ApiError
import dev.materii.gloom.api.util.ApiFailure
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI Service - Free AI chat
 */
class AIService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val authManager: AuthManager
) {

    companion object {
        private const val API_URL = "https://api.openai-proxy.org/v1/chat/completions"
        private const val DEFAULT_MODEL = "gpt-3.5-turbo"

        // Available models
        val AVAILABLE_MODELS = listOf(
            ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "OpenAI", "Fast and efficient"),
            ModelInfo("gpt-4", "GPT-4", "OpenAI", "Most capable model"),
            ModelInfo("gpt-4-turbo", "GPT-4 Turbo", "OpenAI", "Fast GPT-4"),
            ModelInfo("claude-3-haiku", "Claude 3 Haiku", "Anthropic", "Fast and smart"),
            ModelInfo("claude-3-sonnet", "Claude 3 Sonnet", "Anthropic", "Balanced performance")
        )
    }

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val publisher: String,
        val description: String
    )

    /**
     * Send a chat completion request
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): ApiResponse<ChatCompletionResponse> {
        return try {
            val requestBody = ChatCompletionRequest(
                messages = messages,
                model = model,
                temperature = temperature,
                maxTokens = maxTokens
            )

            val response = httpClient.post(API_URL) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(json.encodeToString(requestBody))
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                ApiResponse.Success(json.decodeFromString<ChatCompletionResponse>(body))
            } else {
                val errorBody = response.bodyAsText()
                ApiResponse.Error(ApiError(response.status, errorBody))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResponse.Failure(ApiFailure(e, null))
        }
    }

    /**
     * Get available models
     */
    fun getAvailableModels(): List<ModelInfo> = AVAILABLE_MODELS

    /**
     * Create a system message for coding assistant
     */
    fun createCodingSystemMessage(): ChatMessage = ChatMessage(
        role = "system",
        content = """You are Gloom AI, a helpful coding assistant integrated into Gloom, a GitHub client app.
Help users with coding questions, code reviews, debugging, and GitHub-related topics.
Be concise, helpful, and friendly. Format code with markdown code blocks."""
    )

    /**
     * Create a user message
     */
    fun createUserMessage(content: String): ChatMessage = ChatMessage(
        role = "user",
        content = content
    )

    /**
     * Create an assistant message
     */
    fun createAssistantMessage(content: String): ChatMessage = ChatMessage(
        role = "assistant",
        content = content
    )
}
