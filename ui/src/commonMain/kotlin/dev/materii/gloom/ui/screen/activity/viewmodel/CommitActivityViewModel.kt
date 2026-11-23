package dev.materii.gloom.ui.screen.activity.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.dev.CommitActivityWeek
import dev.materii.gloom.api.dto.dev.CommitItem
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CommitActivityViewModel(
    private val githubRepo: GithubRepository,
    private val input: Input,
) : ScreenModel {

    data class Input(val owner: String, val name: String)

    val commits = mutableStateListOf<CommitItem>()

    var weeks by mutableStateOf<List<CommitActivityWeek>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        if (isLoading) return
        screenModelScope.launch {
            isLoading = true
            hasError = false

            when (val response = githubRepo.getCommits(input.owner, input.name)) {
                is ApiResponse.Success -> {
                    commits.clear()
                    commits.addAll(response.data)
                }
                is ApiResponse.Empty -> commits.clear()
                is ApiResponse.Error -> hasError = true
                is ApiResponse.Failure -> hasError = true
            }
            isLoading = false

            // GitHub computes these stats lazily and answers with an empty 202 at first.
            repeat(4) { attempt ->
                val stats = githubRepo.getCommitActivity(input.owner, input.name)
                if (stats is ApiResponse.Success && stats.data.isNotEmpty()) {
                    weeks = stats.data
                    return@launch
                }
                if (attempt < 3) delay(2_500)
            }
        }
    }
}
