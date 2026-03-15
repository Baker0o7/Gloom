package dev.materii.gloom.api.repository

import dev.materii.gloom.api.dto.chat.ChatMessage
import dev.materii.gloom.api.service.ChatApiService
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.api.util.transform

class ChatRepository(
    private val service: ChatApiService,
) {

    /**
     * GitHub context injected into every system prompt.
     * Set by any screen before opening the chat.
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
     * Send a user message and return the assistant reply as a plain String.
     * History snapshot is taken before appending the new user message.
     */
    suspend fun sendMessage(
        history: List<ChatMessage>,
        userText: String,
    ): ApiResponse<String> =
        service.chat(
            history      = history,
            userText     = userText,
            systemPrompt = buildSystemPrompt(),
        ).transform { response ->
            response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.joinToString("") { it.text }
                ?.trim()
                ?: ""
        }

    /** True when a Gemini API key is present at build time. */
    val hasApiKey: Boolean
        get() = try {
            dev.materii.gloom.api.BuildConfig.GEMINI_API_KEY.isNotBlank()
        } catch (_: Exception) { false }

    // ─── System prompt ────────────────────────────────────────────────────────

    private fun buildSystemPrompt(): String = buildString {
        append(
            """
            You are Gloom AI, an Android & Kotlin development assistant built into the Gloom GitHub client.
            
            Your strengths:
            - Android development (Jetpack Compose, MVVM, Coroutines, Flow, Hilt/Koin, Room, Retrofit/Ktor)
            - Kotlin idioms, extension functions, DSLs, coroutines, sealed classes
            - GitHub workflows: issues, PRs, code review, CI/CD
            - Explaining code, finding bugs, suggesting improvements
            
            Rules:
            - Keep responses concise and direct
            - For code, always specify the language in fenced blocks (```kotlin, ```xml, ```gradle etc.)
            - Prefer Kotlin over Java in all examples
            - When explaining code snippets, point out Kotlin-idiomatic improvements
            """.trimIndent()
        )

        val ctx = context
        if (ctx.repoOwner != null && ctx.repoName != null) {
            append("\n\n## Active Repository\n")
            append("**${ctx.repoOwner}/${ctx.repoName}**")
            ctx.repoDescription?.let { append(" — $it") }
            append("\n")
            ctx.repoLanguage?.let  { append("Language: $it\n") }
            ctx.repoStars?.let     { append("Stars: $it\n") }
            ctx.openIssueCount?.let { append("Open issues: $it\n") }
            ctx.openPrCount?.let   { append("Open PRs: $it\n") }
        }

        if (ctx.currentFilePath != null) {
            append("\n\n## Currently Viewed File\n")
            append("`${ctx.currentFilePath}`\n")
            ctx.currentFileContent?.let { content ->
                val preview = content.lines().take(150).joinToString("\n")
                val lang = when {
                    ctx.currentFilePath.endsWith(".kt")    -> "kotlin"
                    ctx.currentFilePath.endsWith(".xml")   -> "xml"
                    ctx.currentFilePath.endsWith(".gradle") ||
                    ctx.currentFilePath.endsWith(".kts")   -> "kotlin"
                    ctx.currentFilePath.endsWith(".java")  -> "java"
                    else                                   -> ""
                }
                append("```$lang\n$preview\n```\n")
            }
        }
    }
}
