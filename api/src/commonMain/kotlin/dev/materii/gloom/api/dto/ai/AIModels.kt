package dev.materii.gloom.api.dto.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chat message for AI conversations
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

/**
 * Chat completion request for AI API
 */
@Serializable
data class ChatCompletionRequest(
    val messages: List<ChatMessage>,
    val model: String,
    val temperature: Double = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int = 4096,
    @SerialName("top_p")
    val topP: Double = 1.0,
    val stream: Boolean = false
)

/**
 * Chat completion response from AI API
 */
@Serializable
data class ChatCompletionResponse(
    val id: String = "",
    val choices: List<ChatChoice> = emptyList(),
    val created: Long = 0L,
    val model: String = "",
    val usage: Usage? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)
