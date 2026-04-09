package dev.materii.gloom.ui.screen.issue.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse

class CreateIssueViewModel(
    private val githubRepo: GithubRepository,
    private val owner: String,
    private val repo: String,
) : ScreenModel {

    var isSubmitting by mutableStateOf(false)
        private set

    /** Returns true on success */
    suspend fun submit(title: String, body: String): Boolean {
        isSubmitting = true
        val result = githubRepo.createIssue(owner, repo, title, body)
        isSubmitting = false
        return result !is ApiResponse.Error && result !is ApiResponse.Failure
    }
}
