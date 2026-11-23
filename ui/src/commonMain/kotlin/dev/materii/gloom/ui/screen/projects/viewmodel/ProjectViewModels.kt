package dev.materii.gloom.ui.screen.projects.viewmodel

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

data class ProjectSummary(
    val id: String,
    val number: Int,
    val title: String,
    val description: String,
    val closed: Boolean,
    val itemCount: Int,
)

data class ProjectItem(
    val title: String,
    val type: String,
    val number: Int?,
    val state: String?,
)

/** Items sharing one value of the project's "Status" field: a board column. */
data class ProjectColumn(val name: String, val items: List<ProjectItem>)

/** Human-friendly message for a failed GraphQL response (token scope problems are common). */
private fun JsonObject.errorHint(): String {
    val message = arr("errors")?.firstOrNull().str("message").orEmpty()
    return if (message.contains("scope", ignoreCase = true) || message.contains("permission", ignoreCase = true))
        "Your GitHub token needs the read:project permission to show Projects."
    else "Couldn't load projects."
}

class ProjectsViewModel(
    private val githubRepo: GithubRepository,
    private val input: Input,
) : ScreenModel {

    data class Input(val owner: String, val name: String)

    val projects = mutableStateListOf<ProjectSummary>()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            isLoading = true
            errorMessage = null
            val variables = buildJsonObject {
                put("owner", input.owner)
                put("name", input.name)
            }
            when (val response = githubRepo.graphql(QUERY, variables)) {
                is ApiResponse.Success -> {
                    val repo = response.data.obj("data").obj("repository")
                    if (repo == null) {
                        errorMessage = response.data.errorHint()
                    } else {
                        projects.clear()
                        projects.addAll(repo.nodes("projectsV2").map { node ->
                            ProjectSummary(
                                id = node.str("id").orEmpty(),
                                number = node.int("number") ?: 0,
                                title = node.str("title").orEmpty(),
                                description = node.str("shortDescription").orEmpty(),
                                closed = node.bool("closed") ?: false,
                                itemCount = node.obj("items").int("totalCount") ?: 0,
                            )
                        })
                    }
                }
                is ApiResponse.Empty -> projects.clear()
                is ApiResponse.Error -> errorMessage = "Couldn't load projects."
                is ApiResponse.Failure -> errorMessage = "Network error. Please try again."
            }
            isLoading = false
        }
    }

    private companion object {
        const val QUERY = """
            query(${'$'}owner: String!, ${'$'}name: String!) {
              repository(owner: ${'$'}owner, name: ${'$'}name) {
                projectsV2(first: 20) {
                  nodes { id number title shortDescription closed items { totalCount } }
                }
              }
            }
        """
    }
}

class ProjectBoardViewModel(
    private val githubRepo: GithubRepository,
    private val input: Input,
) : ScreenModel {

    data class Input(val projectId: String)

    var columns by mutableStateOf<List<ProjectColumn>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            isLoading = true
            errorMessage = null
            val variables = buildJsonObject { put("id", input.projectId) }
            when (val response = githubRepo.graphql(QUERY, variables)) {
                is ApiResponse.Success -> {
                    val node = response.data.obj("data").obj("node")
                    if (node == null) {
                        errorMessage = response.data.errorHint()
                    } else {
                        // Group by Status, keeping the order columns first appear in.
                        val grouped = LinkedHashMap<String, MutableList<ProjectItem>>()
                        node.nodes("items").forEach { item ->
                            val status = item.obj("fieldValueByName").str("name") ?: "No status"
                            val content = item.obj("content")
                            val type = content.str("__typename") ?: "DraftIssue"
                            val title = content.str("title") ?: "(untitled)"
                            grouped.getOrPut(status) { mutableListOf() }.add(
                                ProjectItem(title, type, content.int("number"), content.str("state"))
                            )
                        }
                        columns = grouped.map { (name, items) -> ProjectColumn(name, items) }
                    }
                }
                is ApiResponse.Empty -> columns = emptyList()
                is ApiResponse.Error -> errorMessage = "Couldn't load this project."
                is ApiResponse.Failure -> errorMessage = "Network error. Please try again."
            }
            isLoading = false
        }
    }

    private companion object {
        const val QUERY = """
            query(${'$'}id: ID!) {
              node(id: ${'$'}id) {
                ... on ProjectV2 {
                  items(first: 100) {
                    nodes {
                      fieldValueByName(name: "Status") {
                        ... on ProjectV2ItemFieldSingleSelectValue { name }
                      }
                      content {
                        __typename
                        ... on Issue { title number state }
                        ... on PullRequest { title number state }
                        ... on DraftIssue { title }
                      }
                    }
                  }
                }
              }
            }
        """
    }
}
