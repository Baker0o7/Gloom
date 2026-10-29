package dev.materii.gloom.ui.screen.explore.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.repository.GraphQLRepository
import dev.materii.gloom.api.util.fold
import dev.materii.gloom.domain.manager.PreferenceManager
import dev.materii.gloom.domain.manager.enums.TrendingPeriodPreference
import dev.materii.gloom.gql.fragment.TrendingRepository
import dev.materii.gloom.gql.type.TrendingPeriod
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val graphQLRepository: GraphQLRepository,
    private val preferenceManager: PreferenceManager
): ScreenModel {

    val trendingRepos = mutableStateListOf<TrendingRepository>()

    val repoStarsPending = mutableStateListOf<String>()

    var isLoading by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    private var loadJob: Job? = null

    var trendingPeriod by mutableStateOf(preferenceManager.trendingPeriod)
        private set

    init {
        loadTrending()
    }

    // PUBLIC

    fun loadTrending() {
        // Cancel any in-flight load so a slow older response can't overwrite a newer one
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            isLoading = true
            hasError = false
            graphQLRepository.getTrending(trendingPeriod.toApi(), language = "Kotlin").fold(
                onSuccess = { trending, _ ->
                    trendingRepos.clear()
                    trendingRepos.addAll(trending)
                },
                onError = { if (trendingRepos.isEmpty()) hasError = true },
                onFailure = { if (trendingRepos.isEmpty()) hasError = true }
            )
            isLoading = false
        }
    }

    fun updateTrendingPeriod(newPeriod: TrendingPeriodPreference) {
        trendingPeriod = newPeriod
        preferenceManager.trendingPeriod = newPeriod
        trendingRepos.clear()
        loadTrending()
    }

    fun starRepo(id: String) {
        screenModelScope.launch {
            repoStarsPending.add(id)
            editRepo(id) { it.copy(viewerHasStarred = true) }
            graphQLRepository.starRepo(id).fold(
                onSuccess = { _, _ -> },
                onError = {
                    editRepo(id) { it.copy(viewerHasStarred = false) }
                },
                onFailure = {
                    editRepo(id) { it.copy(viewerHasStarred = false) }
                }
            )
            repoStarsPending.remove(id)
        }
    }

    fun unstarRepo(id: String) {
        screenModelScope.launch {
            repoStarsPending.add(id)
            editRepo(id) { it.copy(viewerHasStarred = false) }
            graphQLRepository.unstarRepo(id).fold(
                onSuccess = { _, _ -> },
                onError = {
                    editRepo(id) { it.copy(viewerHasStarred = true) }
                },
                onFailure = {
                    editRepo(id) { it.copy(viewerHasStarred = true) }
                }
            )
            repoStarsPending.remove(id)
        }
    }

    // PRIVATE

    private fun editRepo(id: String, block: (TrendingRepository) -> TrendingRepository) {
        // The list may have been reloaded while a star request was in flight
        val targetIndex = trendingRepos.indexOfFirst { it.id == id }
        if (targetIndex < 0) return
        trendingRepos[targetIndex] = block(trendingRepos[targetIndex])
    }

    private fun TrendingPeriodPreference.toApi(): TrendingPeriod {
        return when (this) {
            TrendingPeriodPreference.MONTHLY -> TrendingPeriod.MONTHLY
            TrendingPeriodPreference.WEEKLY  -> TrendingPeriod.WEEKLY
            TrendingPeriodPreference.DAILY   -> TrendingPeriod.DAILY
        }
    }

}