package dev.materii.gloom.ui.screen.search.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.repository.GraphQLRepository
import dev.materii.gloom.api.util.GraphQLResponse
import dev.materii.gloom.gql.SearchQuery
import dev.materii.gloom.gql.type.SearchType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repo: GraphQLRepository,
) : ScreenModel {

    enum class Tab { REPOSITORIES, USERS }

    val results = mutableStateListOf<SearchQuery.Node>()

    var query     by mutableStateOf("")
    var activeTab by mutableStateOf(Tab.REPOSITORIES)
    var isLoading by mutableStateOf(false)
    var error     by mutableStateOf<String?>(null)
    var hasMore   by mutableStateOf(false)
        private set

    private var debounceJob: Job? = null
    private var cursor: String?   = null

    fun onQueryChange(q: String) {
        query = q
        debounceJob?.cancel()
        if (q.isBlank()) { results.clear(); error = null; return }
        debounceJob = screenModelScope.launch {
            delay(350)
            search(reset = true)
        }
    }

    fun onTabChange(tab: Tab) {
        activeTab = tab
        if (query.isNotBlank()) search(reset = true)
    }

    fun loadMore() {
        if (!hasMore || isLoading) return
        search(reset = false)
    }

    private fun search(reset: Boolean) {
        if (reset) { cursor = null; results.clear() }
        val type = if (activeTab == Tab.REPOSITORIES) SearchType.REPOSITORY else SearchType.USER
        screenModelScope.launch {
            isLoading = true
            error     = null
            val response = repo.search(query.trim(), type, cursor)
            when (response) {
                is GraphQLResponse.Success -> {
                    val nodes = response.data.search.nodes?.filterNotNull() ?: emptyList()
                    results.addAll(nodes)
                    cursor  = response.data.search.pageInfo.endCursor
                    hasMore = response.data.search.pageInfo.hasNextPage
                }
                is GraphQLResponse.Error   -> error = "Search failed. Try again."
                is GraphQLResponse.Failure -> error = "Network error. Try again."
            }
            isLoading = false
        }
    }
}
