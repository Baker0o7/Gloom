package dev.materii.gloom.api.repository

import dev.materii.gloom.api.dto.chat.ChatMessage
import dev.materii.gloom.api.service.ChatApiService
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.api.util.ApiError
import dev.materii.gloom.api.util.transform
import dev.materii.gloom.domain.manager.PreferenceManager

class ChatRepository(
    private val service: ChatApiService,
    private val prefs: PreferenceManager,
) {

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

    /** The active API key: user's saved key takes priority over the build-time key. */
    val activeApiKey: String
        get() = prefs.geminiApiKey.trim().ifBlank { service.buildTimeApiKey }

    /** True when at least one key is available. */
    val hasApiKey: Boolean
        get() = activeApiKey.isNotBlank()

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userText: String,
    ): ApiResponse<String> {
        val key = activeApiKey
        if (key.isBlank()) {
            return ApiResponse.Error(ApiError(io.ktor.http.HttpStatusCode.Unauthorized, "No Gemini API key configured"))
        }
        return service.chat(
            history      = history,
            userText     = userText,
            systemPrompt = buildSystemPrompt(),
            apiKey       = key,
        ).transform { response ->
            response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.joinToString("") { it.text }
                ?.trim()
                ?: ""
        }
    }

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
                    ctx.currentFilePath.endsWith(".kt")  || ctx.currentFilePath.endsWith(".kts") -> "kotlin"
                    ctx.currentFilePath.endsWith(".xml")  -> "xml"
                    ctx.currentFilePath.endsWith(".java") -> "java"
                    else                                  -> ""
                }
                append("```$lang\n$preview\n```\n")
            }
        }
    }
}
