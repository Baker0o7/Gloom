package dev.materii.gloom.api.service

import dev.materii.gloom.api.dto.chat.ChatMessage
import dev.materii.gloom.api.dto.chat.GeminiContent
import dev.materii.gloom.api.dto.chat.GeminiPart
import dev.materii.gloom.api.dto.chat.GeminiRequest
import dev.materii.gloom.api.dto.chat.GeminiResponse
import dev.materii.gloom.api.dto.chat.GeminiSystemInstruction
import dev.materii.gloom.api.util.ApiResponse
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatApiService(
    private val client: HttpService,
    private val apiKey: String,
) {

    companion object {
        // Gemini 2.0 Flash — completely free, no credit card required
        // Free tier: 15 RPM · 1 000 000 TPM · 1 500 RPD
        // https://ai.google.dev/gemini-api/docs/models
        private const val MODEL    = "gemini-2.0-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    suspend fun chat(
        history: List<ChatMessage>,
        userText: String,
        systemPrompt: String,
    ): ApiResponse<GeminiResponse> = withContext(Dispatchers.IO) {
        // Build contents: past history + new user turn
        val contents = history.map { msg ->
            GeminiContent(
                role  = if (msg.role == ChatMessage.Role.USER) "user" else "model",
                parts = listOf(GeminiPart(msg.content))
            )
        } + GeminiContent(
            role  = "user",
            parts = listOf(GeminiPart(userText))
        )

        client.request {
            url("$BASE_URL/$MODEL:generateContent?key=$apiKey")
            method = HttpMethod.Post
            setBody(
                GeminiRequest(
                    contents          = contents,
                    systemInstruction = GeminiSystemInstruction(listOf(GeminiPart(systemPrompt))),
                )
            )
        }
    }
}
