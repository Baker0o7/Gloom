package dev.materii.gloom.api.service

import dev.materii.gloom.api.dto.chat.ChatMessageDto
import dev.materii.gloom.api.dto.chat.ChatRequest
import dev.materii.gloom.api.dto.chat.ChatResponse
import dev.materii.gloom.api.util.ApiResponse
import io.ktor.client.request.header
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
        const val BASE_URL = "https://api.anthropic.com"
        const val MODEL    = "claude-haiku-4-5-20251001"
        const val VERSION  = "2023-06-01"
    }

    suspend fun chat(
        messages: List<ChatMessageDto>,
        systemPrompt: String? = null,
        maxTokens: Int = 1024,
    ): ApiResponse<ChatResponse> = withContext(Dispatchers.IO) {
        client.request {
            url("$BASE_URL/v1/messages")
            method = HttpMethod.Post
            header("x-api-key", apiKey)
            header("anthropic-version", VERSION)
            setBody(
                ChatRequest(
                    model      = MODEL,
                    maxTokens  = maxTokens,
                    system     = systemPrompt,
                    messages   = messages,
                    stream     = false,
                )
            )
        }
    }
}
