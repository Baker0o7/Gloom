package dev.materii.gloom.ui.screen.issue.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.api.util.ApiResponse
import kotlinx.coroutines.launch

class CreateIssueViewModel(
    private val githubRepo: GithubRepository,
    private val aiService: AIService,
    private val owner: String,
    private val repo: String,
) : ScreenModel {

    var isSubmitting by mutableStateOf(false)
        private set

    var aiLoading by mutableStateOf(false)
        private set

    /** Returns true on success */
    suspend fun submit(title: String, body: String): Boolean {
        isSubmitting = true
        val result = githubRepo.createIssue(owner, repo, title, body)
        isSubmitting = false
        return result !is ApiResponse.Error && result !is ApiResponse.Failure
    }

    /** Calls AI to draft an issue body from the title, then calls [onResult] on the main thread. */
    fun aiDraftBody(title: String, onResult: (String) -> Unit) {
        if (title.isBlank()) return
        screenModelScope.launch {
            aiLoading = true
            val messages = listOf(
                ChatMessage("system", "You are a helpful GitHub issue writing assistant. Write clear, concise issue descriptions in Markdown."),
                ChatMessage("user", "Write a GitHub issue body for an issue titled: \"$title\" in the $owner/$repo repository. Include steps to reproduce if it sounds like a bug, or acceptance criteria if it sounds like a feature request. Keep it under 200 words.")
            )
            val result = aiService.chat(messages = messages, maxTokens = 512)
            if (result is ApiResponse.Success) {
                val text = result.data.choices.firstOrNull()?.message?.content?.trim() ?: ""
                if (text.isNotBlank()) onResult(text)
            }
            aiLoading = false
        }
    }
}
