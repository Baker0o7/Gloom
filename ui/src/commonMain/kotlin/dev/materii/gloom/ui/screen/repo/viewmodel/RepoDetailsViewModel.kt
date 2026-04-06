package dev.materii.gloom.ui.screen.repo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.repository.GraphQLRepository
import dev.materii.gloom.api.util.fold
import dev.materii.gloom.api.util.ifUnsuccessful
import dev.materii.gloom.gql.fragment.RepoDetails
import kotlinx.coroutines.launch

class RepoDetailsViewModel(
    private val gql: GraphQLRepository,
    nameWithOwner: Pair<String, String>
): ScreenModel {

    val owner = nameWithOwner.first
    val name = nameWithOwner.second

    var details by mutableStateOf(null as RepoDetails?)
    var detailsLoading by mutableStateOf(false)
    var hasError by mutableStateOf(false)

    var isStarLoading by mutableStateOf(false)

    init {
        loadDetails()
    }

    fun loadDetails() {
        screenModelScope.launch {
            detailsLoading = true
            gql.getRepoDetails(owner, name).fold(
                onSuccess = {
                    details = it
                    detailsLoading = false
                },
                onError = {
                    hasError = true
                    detailsLoading = false
                }
            )
        }
    }

    private fun updateStarDetails(starred: Boolean) {
        details?.let { d ->
            details = d.copy(
                viewerHasStarred = starred,
                stargazerCount = d.stargazerCount + if (starred) 1 else -1
            )
        }
    }

    fun toggleStar() {
        val currentDetails = details ?: return
        val hasStarred = currentDetails.viewerHasStarred

        // Optimistic update
        updateStarDetails(starred = !hasStarred)

        val detailsId = details?.id
        if (detailsId == null) return

        screenModelScope.launch {
            isStarLoading = true

            if (hasStarred) {
                gql.unstarRepo(detailsId)
            } else {
                gql.starRepo(detailsId)
            }.ifUnsuccessful {
                // Revert optimistic update
                updateStarDetails(starred = hasStarred)
            }
            isStarLoading = false
        }
    }

}