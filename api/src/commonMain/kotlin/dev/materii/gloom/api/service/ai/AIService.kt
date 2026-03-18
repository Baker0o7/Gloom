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
 * AI Service for Z.AI
 * Uses the Z.AI backend for chat completions - powerful AI capabilities
 */
class AIService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val authManager: AuthManager
) {

    companion object {
        // Z.AI Backend URL - can be configured for different environments
        // For local development: http://10.0.2.2:3001 (Android emulator) or http://localhost:3001 (Desktop)
        // For production: Update to your deployed backend URL
        private const val ZAI_BASE_URL = "http://10.0.2.2:3001"
        private const val ZAI_DESKTOP_URL = "http://localhost:3001"
        private const val DEFAULT_MODEL = "default"

        // Available models through Z.AI
        val AVAILABLE_MODELS = listOf(
            ModelInfo("default", "Z.AI Smart", "Z.AI", "Intelligent assistant for coding and general tasks"),
            ModelInfo("code-expert", "Code Expert", "Z.AI", "Specialized for code analysis and generation"),
            ModelInfo("creative", "Creative Writer", "Z.AI", "Best for documentation and explanations"),
            ModelInfo("fast", "Quick Response", "Z.AI", "Optimized for fast, concise answers")
        )
    }

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val publisher: String,
        val description: String
    )

    /**
     * Send a chat completion request to Z.AI Backend
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): ApiResponse<ChatCompletionResponse> {
        // For Z.AI, we don't require GitHub authentication
        // The backend handles the AI authentication

        return try {
            val requestBody = ChatCompletionRequest(
                messages = messages,
                model = model,
                temperature = temperature,
                maxTokens = maxTokens
            )

            // Try Android emulator URL first, then desktop URL
            val baseUrl = tryAndroidUrl()

            println("AI Service: Sending request to $baseUrl/api/chat")
            println("AI Service: Model = $model")
            println("AI Service: Messages count = ${messages.size}")

            val response = httpClient.post("$baseUrl/api/chat") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(json.encodeToString(requestBody))
            }

            println("AI Service: Response status = ${response.status}")

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                println("AI Service: Response body length = ${body.length}")
                try {
                    ApiResponse.Success(json.decodeFromString<ChatCompletionResponse>(body))
                } catch (e: Exception) {
                    println("AI Service: Parse error - ${e.message}")
                    ApiResponse.Error(ApiError(response.status, "Failed to parse response: ${e.message}"))
                }
            } else {
                val errorBody = response.bodyAsText()
                println("AI Service: Error response = $errorBody")
                ApiResponse.Error(ApiError(response.status, errorBody.ifEmpty { "HTTP ${response.status}" }))
            }
        } catch (e: Exception) {
            println("AI Service: Primary URL failed, trying fallback - ${e.message}")
            // Try fallback URL for desktop
            try {
                val requestBody = ChatCompletionRequest(
                    messages = messages,
                    model = model,
                    temperature = temperature,
                    maxTokens = maxTokens
                )

                val response = httpClient.post("$ZAI_DESKTOP_URL/api/chat") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(json.encodeToString(requestBody))
                }

                if (response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    ApiResponse.Success(json.decodeFromString<ChatCompletionResponse>(body))
                } else {
                    val errorBody = response.bodyAsText()
                    ApiResponse.Error(ApiError(response.status, errorBody.ifEmpty { "HTTP ${response.status}" }))
                }
            } catch (fallbackError: Exception) {
                println("AI Service: Fallback also failed - ${fallbackError.message}")
                fallbackError.printStackTrace()
                ApiResponse.Failure(ApiFailure(fallbackError, fallbackError.message))
            }
        }
    }

    /**
     * Get the appropriate base URL for the current platform
     */
    private fun tryAndroidUrl(): String = ZAI_BASE_URL

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
When showing code examples, use markdown code blocks with the language specified.

Be friendly and professional. If you don't know something, admit it honestly."""
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
