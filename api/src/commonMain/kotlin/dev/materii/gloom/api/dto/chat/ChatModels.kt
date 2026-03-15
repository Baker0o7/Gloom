package dev.materii.gloom.api.dto.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Gemini REST API ──────────────────────────────────────────────────────────
// POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null,
    @SerialName("generationConfig")   val generationConfig:  GeminiGenerationConfig   = GeminiGenerationConfig(),
)

@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiContent(
    val role: String,   // "user" | "model"
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String,
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 2048,
    val temperature: Double = 0.7,
)

// ─── Gemini Response ──────────────────────────────────────────────────────────

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
)

// ─── UI model ─────────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
) {
    enum class Role { USER, ASSISTANT }
}

// ─── Parsed message segment (for rendering) ───────────────────────────────────

sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Code(val language: String, val code: String) : MessageSegment()
}

/** Parse a raw AI response string into alternating Text/Code segments. */
fun parseSegments(raw: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val fenceRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
    var last = 0
    for (match in fenceRegex.findAll(raw)) {
        if (match.range.first > last) {
            val text = raw.substring(last, match.range.first).trim()
            if (text.isNotEmpty()) segments += MessageSegment.Text(text)
        }
        val lang = match.groupValues[1].ifBlank { "code" }
        val code = match.groupValues[2].trimEnd()
        segments += MessageSegment.Code(lang, code)
        last = match.range.last + 1
    }
    if (last < raw.length) {
        val text = raw.substring(last).trim()
        if (text.isNotEmpty()) segments += MessageSegment.Text(text)
    }
    return segments.ifEmpty { listOf(MessageSegment.Text(raw)) }
}
