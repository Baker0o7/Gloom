package dev.materii.gloom.ui.screen.discussions.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.api.util.arr
import dev.materii.gloom.api.util.bool
import dev.materii.gloom.api.util.int
import dev.materii.gloom.api.util.nodes
import dev.materii.gloom.api.util.obj
import dev.materii.gloom.api.util.str
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class DiscussionSummary(
    val number: Int,
    val title: String,
    val category: String,
    val emoji: String,
    val author: String,
    val comments: Int,
    val upvotes: Int,
    val answered: Boolean,
    val createdAt: String?,
)

data class DiscussionComment(
    val author: String,
    val body: String,
    val createdAt: String?,
    val upvotes: Int,
)

data class DiscussionDetail(
    val title: String,
    val body: String,
    val author: String,
    val category: String,
    val upvotes: Int,
    val createdAt: String?,
    val answer: DiscussionComment?,
    val comments: List<DiscussionComment>,
)

/** First GraphQL error message in a response, if any. */
internal fun JsonObject.firstError(): String? = arr("errors")?.firstOrNull().str("message")

private val HTML_TAG = Regex("<[^>]*>")

class DiscussionsViewModel(
    private val githubRepo: GithubRepository,
    private val input: Input,
) : ScreenModel {

    data class Input(val owner: String, val name: String)

    val discussions = mutableStateListOf<DiscussionSummary>()

    var isLoading by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    var isEnabled by mutableStateOf(true)
        private set

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            isLoading = true
            hasError = false
            val variables = buildJsonObject {
                put("owner", input.owner)
                put("name", input.name)
            }
            when (val response = githubRepo.graphql(QUERY, variables)) {
                is ApiResponse.Success -> {
                    val repo = response.data.obj("data").obj("repository")
                    if (repo == null) {
                        hasError = true
                    } else {
                        isEnabled = repo.bool("hasDiscussionsEnabled") ?: false
                        discussions.clear()
                        discussions.addAll(repo.nodes("discussions").map { node ->
                            DiscussionSummary(
                                number = node.int("number") ?: 0,
                                title = node.str("title").orEmpty(),
                                category = node.obj("category").str("name").orEmpty(),
                                emoji = node.obj("category").str("emojiHTML").orEmpty().replace(HTML_TAG, "").trim(),
                                author = node.obj("author").str("login") ?: "ghost",
                                comments = node.obj("comments").int("totalCount") ?: 0,
                                upvotes = node.int("upvoteCount") ?: 0,
                                answered = node.bool("isAnswered") ?: false,
                                createdAt = node.str("createdAt"),
                            )
                        })
                    }
                }
                is ApiResponse.Empty -> discussions.clear()
                is ApiResponse.Error -> hasError = true
                is ApiResponse.Failure -> hasError = true
            }
            isLoading = false
        }
    }

    private companion object {
        const val QUERY = """
            query(${'$'}owner: String!, ${'$'}name: String!) {
              repository(owner: ${'$'}owner, name: ${'$'}name) {
                hasDiscussionsEnabled
                discussions(first: 30, orderBy: {field: UPDATED_AT, direction: DESC}) {
                  nodes {
                    number title createdAt isAnswered upvoteCount
                    category { name emojiHTML }
                    author { login }
                    comments { totalCount }
                  }
                }
              }
            }
        """
    }
}

class DiscussionViewModel(
    private val githubRepo: GithubRepository,
    private val input: Input,
) : ScreenModel {

    data class Input(val owner: String, val name: String, val number: Int)

    var detail by mutableStateOf<DiscussionDetail?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            isLoading = true
            hasError = false
            val variables = buildJsonObject {
                put("owner", input.owner)
                put("name", input.name)
                put("number", input.number)
            }
            when (val response = githubRepo.graphql(QUERY, variables)) {
                is ApiResponse.Success -> {
                    val node = response.data.obj("data").obj("repository").obj("discussion")
                    if (node == null) {
                        hasError = true
                    } else {
                        detail = DiscussionDetail(
                            title = node.str("title").orEmpty(),
                            body = node.str("bodyText").orEmpty(),
                            author = node.obj("author").str("login") ?: "ghost",
                            category = node.obj("category").str("name").orEmpty(),
                            upvotes = node.int("upvoteCount") ?: 0,
                            createdAt = node.str("createdAt"),
                            answer = node.obj("answer")?.let { parseComment(it) },
                            comments = node.nodes("comments").map { parseComment(it) },
                        )
                    }
                }
                is ApiResponse.Empty -> hasError = true
                is ApiResponse.Error -> hasError = true
                is ApiResponse.Failure -> hasError = true
            }
            isLoading = false
        }
    }

    private fun parseComment(node: JsonObject) = DiscussionComment(
        author = node.obj("author").str("login") ?: "ghost",
        body = node.str("bodyText").orEmpty(),
        createdAt = node.str("createdAt"),
        upvotes = node.int("upvoteCount") ?: 0,
    )

    private companion object {
        const val QUERY = """
            query(${'$'}owner: String!, ${'$'}name: String!, ${'$'}number: Int!) {
              repository(owner: ${'$'}owner, name: ${'$'}name) {
                discussion(number: ${'$'}number) {
                  title bodyText createdAt upvoteCount
                  author { login }
                  category { name }
                  answer { bodyText createdAt upvoteCount author { login } }
                  comments(first: 50) {
                    nodes { bodyText createdAt upvoteCount author { login } }
                  }
                }
              }
            }
        """
    }
}
