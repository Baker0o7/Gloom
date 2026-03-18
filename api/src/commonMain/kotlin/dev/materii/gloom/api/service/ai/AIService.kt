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
 * AI Service for GitHub Models API
 * Uses the GitHub token for authentication - completely free for limited use
 */
class AIService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val authManager: AuthManager
) {

    companion object {
        private const val GITHUB_MODELS_BASE_URL = "https://models.inference.ai.azure.com"
        private const val DEFAULT_MODEL = "gpt-4o-mini"

        // Available free models on GitHub Models
        val AVAILABLE_MODELS = listOf(
            ModelInfo("gpt-4o-mini", "GPT-4o Mini", "OpenAI", "Fast and efficient for coding tasks"),
            ModelInfo("gpt-4o", "GPT-4o", "OpenAI", "Most capable model for complex tasks"),
            ModelInfo("Llama-3.3-70B-Instruct", "Llama 3.3 70B", "Meta", "Open-source powerful model"),
            ModelInfo("Phi-4-mini-instruct", "Phi-4 Mini", "Microsoft", "Lightweight reasoning model"),
            ModelInfo("Mistral-large-2411", "Mistral Large", "Mistral AI", "European AI model"),
            ModelInfo("Codestral-2501", "Codestral", "Mistral AI", "Optimized for code generation")
        )
    }

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val publisher: String,
        val description: String
    )

    /**
     * Send a chat completion request to GitHub Models API
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): ApiResponse<ChatCompletionResponse> {
        val token = authManager.authToken

        if (token.isBlank()) {
            return ApiResponse.Error(ApiError(HttpStatusCode.Unauthorized, "Not authenticated. Please sign in first."))
        }

        return try {
            val requestBody = ChatCompletionRequest(
                messages = messages,
                model = model,
                temperature = temperature,
                maxTokens = maxTokens
            )

            val response = httpClient.post("$GITHUB_MODELS_BASE_URL/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $token")
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
        content = """You are an expert coding assistant integrated into Gloom, a GitHub client app.
You help users with:
- Understanding code and repositories
- Writing and debugging code
- Explaining programming concepts
- Answering questions about GitHub features
- Code review and best practices

Provide clear, concise, and helpful responses. Format code blocks with appropriate language tags.
When showing code examples, use markdown code blocks with the language specified."""
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
