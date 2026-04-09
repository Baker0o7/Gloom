package dev.materii.gloom.ui.screen.home.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import app.cash.paging.compose.LazyPagingItems
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.repository.GraphQLRepository
import dev.materii.gloom.api.util.ifSuccessful
import dev.materii.gloom.api.util.fold
import dev.materii.gloom.gql.FeedQuery
import dev.materii.gloom.gql.type.ReactionContent
import dev.materii.gloom.ui.screen.list.viewmodel.BaseListViewModel
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: GraphQLRepository
): BaseListViewModel<FeedQuery.Node, FeedQuery.Data?>() {

    val starredRepos = mutableStateMapOf<String, Pair<Boolean, Int>>()
    val followedUsers = mutableStateMapOf<String, Pair<Boolean, Int>>()

    fun refresh(pagingData: LazyPagingItems<FeedQuery.Node>) {
        starredRepos.clear()
        followedUsers.clear()
        pagingData.refresh()
    }

    override suspend fun loadPage(cursor: String?): FeedQuery.Data? =
        repo.getFeed(cursor).fold(
            onSuccess = { data, _ -> data },
            onError = { throw IllegalStateException("Failed to load home feed") },
            onFailure = { throw it }
        )

    override fun getCursor(data: FeedQuery.Data?): String? =
        data?.viewer?.dashboard?.feed?.items?.pageInfo?.endCursor

    override fun createItems(data: FeedQuery.Data?): List<FeedQuery.Node> {
        return data?.viewer?.dashboard?.feed?.items?.nodes
            ?.filterNotNull()
            ?.filter { it.isSupportedHomeItem() }
            ?: emptyList()
    }

    fun starRepo(id: String) = screenModelScope.launch {
        repo.starRepo(id).ifSuccessful {
            starredRepos[id] = it
        }
    }

    fun unstarRepo(id: String) = screenModelScope.launch {
        repo.unstarRepo(id).ifSuccessful {
            starredRepos[id] = it
        }
    }

    fun followUser(id: String) = screenModelScope.launch {
        repo.followUser(id).ifSuccessful {
            followedUsers[id] = it
        }
    }

    fun unfollowUser(id: String) = screenModelScope.launch {
        repo.unfollowUser(id).ifSuccessful {
            followedUsers[id] = it
        }
    }

    fun react(id: String, reaction: ReactionContent, unreact: Boolean) {
        screenModelScope.launch {
            if (unreact) {
                repo.unreact(id, reaction)
            } else {
                repo.react(id, reaction)
            }
        }
    }

}

private fun FeedQuery.Node.isSupportedHomeItem(): Boolean = when {
    createdRepoItemFragment != null -> true
    newReleaseItemFragment != null -> true
    followedUserFeedItemFragment != null ->
        followedUserFeedItemFragment?.followee?.feedUser?.id != null ||
            followedUserFeedItemFragment?.followee?.feedOrg?.id != null
    starredFeedItemFragment != null -> true
    forkedRepositoryFeedItemFragment != null -> true
    mergedPullRequestFeedItemFragment != null ->
        mergedPullRequestFeedItemFragment?.pullRequest?.baseRepository != null
    else -> false
}
