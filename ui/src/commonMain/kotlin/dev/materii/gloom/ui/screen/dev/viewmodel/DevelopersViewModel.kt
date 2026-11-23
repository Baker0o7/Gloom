package dev.materii.gloom.ui.screen.dev.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.dev.UserSummary
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DevelopersViewModel(
    private val githubRepo: GithubRepository,
) : ScreenModel {

    enum class Filter(val label: String, val query: String) {
        KOTLIN("Kotlin", "language:kotlin followers:>300 type:user"),
        ANDROID("Android", "android in:bio language:kotlin followers:>100 type:user"),
        KENYA("Kenya", "location:kenya followers:>10 type:user"),
        TOP("Top", "followers:>10000 type:user"),
    }

    val developers = mutableStateListOf<UserSummary>()

    var filter by mutableStateOf(Filter.KOTLIN)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    private var job: Job? = null

    init {
        load()
    }

    fun select(newFilter: Filter) {
        if (newFilter == filter) return
        filter = newFilter
        load()
    }

    fun load() {
        job?.cancel()
        job = screenModelScope.launch {
            isLoading = true
            hasError = false
            when (val response = githubRepo.searchUsers(filter.query)) {
                is ApiResponse.Success -> {
                    developers.clear()
                    developers.addAll(response.data.items)
                }
                is ApiResponse.Empty -> developers.clear()
                is ApiResponse.Error -> hasError = true
                is ApiResponse.Failure -> hasError = true
            }
            isLoading = false
        }
    }
}
