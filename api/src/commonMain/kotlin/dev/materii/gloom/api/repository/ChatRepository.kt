package dev.materii.gloom.api.repository

import dev.materii.gloom.api.dto.chat.ChatMessage
import dev.materii.gloom.api.dto.chat.ChatMessageDto
import dev.materii.gloom.api.service.ChatApiService
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.api.util.transform

class ChatRepository(
    private val service: ChatApiService,
) {

    /**
     * Holds per-conversation context that can be set by any screen in Gloom.
     * Cleared when a new conversation starts.
     */
    data class GitHubContext(
        val repoOwner: String? = null,
        val repoName: String? = null,
        val repoDescription: String? = null,
        val repoLanguage: String? = null,
        val repoStars: Int? = null,
        val currentFilePath: String? = null,
        val currentFileContent: String? = null,
        val openIssueCount: Int? = null,
        val openPrCount: Int? = null,
    )

    var context: GitHubContext = GitHubContext()

    /**
     * Send a message and return the assistant reply text.
     */
    suspend fun sendMessage(
        history: List<ChatMessage>,
        userText: String,
    ): ApiResponse<String> {
        val messages = history.map {
            ChatMessageDto(
                role    = if (it.role == ChatMessage.Role.USER) "user" else "assistant",
                content = it.content,
            )
        } + ChatMessageDto(role = "user", content = userText)

        return service.chat(
            messages     = messages,
            systemPrompt = buildSystemPrompt(),
            maxTokens    = 2048,
        ).transform { response ->
            response.content
                .filter { it.type == "text" }
                .joinToString("") { it.text ?: "" }
                .trim()
        }
    }

    private fun buildSystemPrompt(): String {
        val ctx = context
        val sb = StringBuilder()

        sb.append(
            """
            You are Gloom AI, a helpful assistant built into the Gloom GitHub client app for Android.
            You specialize in GitHub — repositories, issues, pull requests, code review, and open-source best practices.
            Keep responses concise and use Markdown formatting where it helps readability.
            When explaining code, be specific and actionable.
            """.trimIndent()
        )

        // Inject current context if available
        if (ctx.repoOwner != null && ctx.repoName != null) {
            sb.append("\n\n## Current Repository Context\n")
            sb.append("Repository: **${ctx.repoOwner}/${ctx.repoName}**\n")
            ctx.repoDescription?.let { sb.append("Description: $it\n") }
            ctx.repoLanguage?.let   { sb.append("Primary language: $it\n") }
            ctx.repoStars?.let      { sb.append("Stars: $it\n") }
            ctx.openIssueCount?.let { sb.append("Open issues: $it\n") }
            ctx.openPrCount?.let    { sb.append("Open PRs: $it\n") }
        }

        if (ctx.currentFilePath != null) {
            sb.append("\n\n## Currently Viewed File\n")
            sb.append("Path: `${ctx.currentFilePath}`\n")
            ctx.currentFileContent?.let { content ->
                val preview = content.lines().take(120).joinToString("\n")
                sb.append("Content:\n```\n$preview\n```\n")
            }
        }

        return sb.toString()
    }
}
