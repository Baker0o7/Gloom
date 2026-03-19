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
 * Chat completion request for GitHub Models API
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
 * Chat completion response from GitHub Models API
 */
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<ChatChoice>,
    val created: Long,
    val model: String,
    val usage: Usage? = null
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)
