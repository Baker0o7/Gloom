package dev.materii.gloom.api.service.ai

import dev.materii.gloom.api.dto.ai.*
import dev.materii.gloom.api.util.ApiError
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import dev.materii.gloom.domain.manager.PreferenceManager
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI Service for Z.AI
 * Uses the Z.AI backend for chat completions
 */
class AIService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val authManager: AuthManager,
    private val prefs: PreferenceManager
) {

    companion object {
        private const val ANDROID_EMULATOR_URL = "http://10.0.2.2:3001"
        private const val DESKTOP_URL = "http://localhost:3001"
        private const val DEFAULT_MODEL = "default"

        val AVAILABLE_MODELS = listOf(
            ModelInfo("default", "Z.AI Smart", "Z.AI", "Intelligent assistant for coding and general tasks"),
            ModelInfo("code-expert", "Code Expert", "Z.AI", "Specialized for code analysis"),
            ModelInfo("creative", "Creative Writer", "Z.AI", "Best for documentation"),
            ModelInfo("fast", "Quick Response", "Z.AI", "Fast, concise answers")
        )
    }

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val publisher: String,
        val description: String
    )

    private fun getApiUrl(): String {
        val customUrl = prefs.aiApiUrl.trim()
        return if (customUrl.isNotBlank()) {
            customUrl.trimEnd('/')
        } else {
            ANDROID_EMULATOR_URL
        }
    }

    fun isEnabled(): Boolean = prefs.aiEnabled
    fun getConfiguredUrl(): String = getApiUrl()

    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): ApiResponse<ChatCompletionResponse> {
        val requestBody = ChatCompletionRequest(
            messages = messages,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens
        )

        val urlsToTry = mutableListOf<String>()
        val primaryUrl = getApiUrl()
        urlsToTry.add(primaryUrl)
        
        // Add fallbacks
        if (primaryUrl != DESKTOP_URL) urlsToTry.add(DESKTOP_URL)
        if (primaryUrl != ANDROID_EMULATOR_URL) urlsToTry.add(ANDROID_EMULATOR_URL)

        for (url in urlsToTry) {
            val result = tryRequest(url, requestBody)
            if (result != null) return result
        }

        // Helpful error message
        val errorMsg = buildString {
            append("Cannot connect to AI backend.\n\n")
            append("Tried URLs:\n")
            urlsToTry.forEach { append("• $it\n") }
            append("\nSolutions:\n")
            append("1. Start backend: cd ai-backend && npm run dev\n")
            append("2. For real device: Settings > AI > set PC IP\n")
            append("   Example: http://192.168.1.100:3001\n")
            append("3. For emulator: keep default (10.0.2.2:3001)")
        }
        
        return ApiResponse.Error(ApiError(HttpStatusCode.ServiceUnavailable, errorMsg))
    }

    private suspend fun tryRequest(
        baseUrl: String,
        requestBody: ChatCompletionRequest
    ): ApiResponse<ChatCompletionResponse>? {
        return try {
            val response = httpClient.post("$baseUrl/api/chat") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(json.encodeToString(requestBody))
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsed = json.decodeFromString<ChatCompletionResponse>(body)
                
                if (parsed.choices.isNotEmpty() && 
                    parsed.choices[0].message?.content?.isNotBlank() == true) {
                    ApiResponse.Success(parsed)
                } else {
                    ApiResponse.Error(ApiError(response.status, "Empty response from AI"))
                }
            } else {
                val errorBody = response.bodyAsText()
                ApiResponse.Error(ApiError(response.status, errorBody.ifBlank { "HTTP ${response.status}" }))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAvailableModels(): List<ModelInfo> = AVAILABLE_MODELS

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

    fun createUserMessage(content: String): ChatMessage = ChatMessage(
        role = "user",
        content = content
    )

    fun createAssistantMessage(content: String): ChatMessage = ChatMessage(
        role = "assistant",
        content = content
    )
}
