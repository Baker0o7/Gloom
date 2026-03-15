package dev.materii.gloom.api.dto.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Outbound ─────────────────────────────────────────────────────────────────

@Serializable
data class ChatRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    val system: String? = null,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
)

@Serializable
data class ChatMessageDto(
    val role: String,      // "user" | "assistant"
    val content: String,
)

// ─── Inbound ──────────────────────────────────────────────────────────────────

@Serializable
data class ChatResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: Usage,
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null,
)

@Serializable
data class Usage(
    @SerialName("input_tokens")  val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int,
)

// ─── UI model ─────────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
) {
    enum class Role { USER, ASSISTANT }
}
