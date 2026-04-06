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
import kotlinx.serialization.json.Json

/**
 * AI Service supporting multiple LLM providers (OpenAI-compatible).
 *
 * Default: SiliconFlow (free Qwen models)
 * Supported: OpenAI, Anthropic, Google, Groq, etc.
 * Users can override the base URL in Settings → AI Settings.
 */
class AIService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val authManager: AuthManager,
    private val prefs: PreferenceManager,
) {

    companion object {
        // Default: SiliconFlow (free)
        const val DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1"
        const val DEFAULT_MODEL    = "Qwen/Qwen3-30B-A3B:free"

        val AVAILABLE_MODELS = listOf(
            // SiliconFlow (Free)
            ModelInfo("Qwen/Qwen3-30B-A3B:free", "Qwen 3.6 Plus (Free)", "Alibaba", "Free"),
            ModelInfo("Qwen/Qwen2.5-Coder-32B-Instruct:free", "Qwen 2.5 Coder (Free)", "Alibaba", "Free - coding"),
            ModelInfo("deepseek-ai/DeepSeek-V2.5:free", "DeepSeek V2.5 (Free)", "DeepSeek", "Free"),
            ModelInfo("THUDM/GLM-4-Flash:free", "GLM-4 Flash (Free)", "Zhipu AI", "Free"),
            
            // OpenAI (requires API key)
            ModelInfo("gpt-4o", "GPT-4o", "OpenAI", "Latest flagship"),
            ModelInfo("gpt-4o-mini", "GPT-4o Mini", "OpenAI", "Fast & cheap"),
            ModelInfo("gpt-4-turbo", "GPT-4 Turbo", "OpenAI", "Powerful"),
            
            // Anthropic (requires API key)
            ModelInfo("claude-sonnet-4-20250514", "Claude Sonnet 4", "Anthropic", "Latest"),
            ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Anthropic", "Popular"),
            ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", "Anthropic", "Fast"),
            
            // Google (requires API key)
            ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", "Google", "Multimodal"),
            ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", "Google", "Fast"),
            
            // Groq (requires API key)
            ModelInfo("llama-3.1-70b-versatile", "Llama 3.1 70B", "Groq", "Fast"),
            ModelInfo("mixtral-8x7b-32768", "Mixtral 8x7B", "Groq", "Efficient"),
            
            // Ollama (local/self-hosted)
            ModelInfo("ollama/llama3", "Llama 3", "Ollama", "Local"),
            ModelInfo("ollama/codellama", "Code Llama", "Ollama", "Local coding"),
            ModelInfo("ollama/mistral", "Mistral", "Ollama", "Local"),
        )
    }

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val publisher: String,
        val description: String,
    )

    fun isEnabled(): Boolean = prefs.aiEnabled

    /** Active base URL — user custom URL or the default. */
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
                ApiError(HttpStatusCode.Unauthorized, "No API key configured. Add one in Settings → AI Settings.")
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
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $key")
                setBody(requestBody)
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) {
                val parsed = json.decodeFromString<ChatCompletionResponse>(body)
                val text   = parsed.choices.firstOrNull()?.message?.content?.trim() ?: ""
                if (text.isNotBlank()) ApiResponse.Success(parsed)
                else ApiResponse.Error(ApiError(response.status, "Empty response from AI"))
            } else {
                ApiResponse.Error(ApiError(response.status, "${response.status.value}: ${body.take(200)}"))
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
