package dev.materii.gloom.api.service

import dev.materii.gloom.api.dto.actions.WorkflowJobsResponse
import dev.materii.gloom.api.dto.actions.WorkflowRunsResponse
import dev.materii.gloom.api.dto.content.ContentFile
import dev.materii.gloom.api.dto.dev.CommitActivityWeek
import dev.materii.gloom.api.dto.dev.CommitItem
import dev.materii.gloom.api.dto.dev.UserSearchResponse
import dev.materii.gloom.api.dto.gist.Gist
import dev.materii.gloom.api.dto.release.LatestRelease
import kotlinx.serialization.json.JsonObject
import dev.materii.gloom.api.dto.content.GitRef
import dev.materii.gloom.api.dto.notification.Notification
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GithubApiService(
    private val client: HttpService,
    private val authManager: AuthManager,
) {

    private val base = "https://api.github.com"
    private fun auth() = "Bearer ${authManager.authToken}"

    // ── Notifications ─────────────────────────────────────────────────────────

    suspend fun getNotifications(
        all: Boolean = false,
        page: Int = 1,
        perPage: Int = 50,
    ): ApiResponse<List<Notification>> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/notifications?all=$all&page=$page&per_page=$perPage")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Get
        }
    }

    suspend fun markThreadRead(threadId: String): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/notifications/threads/$threadId")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Patch
            }
        }

    suspend fun markAllRead(): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/notifications")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Put
            }
        }

    // ── Issues ────────────────────────────────────────────────────────────────

    @Serializable
    private data class CreateIssueBody(val title: String, val body: String)

    suspend fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String,
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/repos/$owner/$repo/issues")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Post
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CreateIssueBody(title, body))
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    suspend fun getWorkflowRuns(
        owner: String,
        repo: String,
        perPage: Int = 30,
    ): ApiResponse<WorkflowRunsResponse> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/repos/$owner/$repo/actions/runs?per_page=$perPage")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Get
        }
    }

    suspend fun getRunJobs(
        owner: String,
        repo: String,
        runId: Long,
    ): ApiResponse<WorkflowJobsResponse> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/repos/$owner/$repo/actions/runs/$runId/jobs")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Get
        }
    }

    suspend fun cancelRun(owner: String, repo: String, runId: Long): ApiResponse<String> =
        postAction("$base/repos/$owner/$repo/actions/runs/$runId/cancel")

    suspend fun rerunRun(owner: String, repo: String, runId: Long): ApiResponse<String> =
        postAction("$base/repos/$owner/$repo/actions/runs/$runId/rerun")

    suspend fun rerunFailedJobs(owner: String, repo: String, runId: Long): ApiResponse<String> =
        postAction("$base/repos/$owner/$repo/actions/runs/$runId/rerun-failed-jobs")

    /** Plain-text log of a single job (GitHub answers with a redirect to the log file). */
    suspend fun getJobLogs(owner: String, repo: String, jobId: Long): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/repos/$owner/$repo/actions/jobs/$jobId/logs")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Get
            }
        }

    private suspend fun postAction(endpoint: String): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url(endpoint)
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Post
            }
        }

    // ── Contents / branches (code editor) ────────────────────────────────────

    @Serializable
    private data class CommitFileBody(
        val message: String,
        val content: String,
        val sha: String,
        val branch: String,
    )

    @Serializable
    private data class CreateRefBody(val ref: String, val sha: String)

    private fun encodePath(path: String) =
        path.split("/").joinToString("/") { it.encodeURLPathPart() }

    suspend fun getFileContent(
        owner: String,
        repo: String,
        path: String,
        ref: String,
    ): ApiResponse<ContentFile> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/repos/$owner/$repo/contents/${encodePath(path)}?ref=${ref.encodeURLParameter()}")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Get
        }
    }

    /** [contentBase64] is the new file content, base64 encoded; [sha] is the blob sha being replaced. */
    suspend fun updateFile(
        owner: String,
        repo: String,
        path: String,
        message: String,
        contentBase64: String,
        sha: String,
        branch: String,
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/repos/$owner/$repo/contents/${encodePath(path)}")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Put
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CommitFileBody(message, contentBase64, sha, branch))
        }
    }

    suspend fun getBranchRef(owner: String, repo: String, branch: String): ApiResponse<GitRef> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/repos/$owner/$repo/git/ref/heads/${encodePath(branch)}")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Get
            }
        }

    suspend fun createBranch(
        owner: String,
        repo: String,
        newBranch: String,
        fromSha: String,
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/repos/$owner/$repo/git/refs")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Post
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CreateRefBody("refs/heads/$newBranch", fromSha))
        }
    }

    // ── Gists ─────────────────────────────────────────────────────────────────

    @Serializable
    private data class CreateGistBody(
        val description: String,
        @SerialName("public") val isPublic: Boolean,
        val files: Map<String, GistFileBody>,
    )

    @Serializable
    private data class GistFileBody(val content: String)

    suspend fun getGists(perPage: Int = 50): ApiResponse<List<Gist>> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/gists?per_page=$perPage")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Get
        }
    }

    suspend fun getLatestRelease(owner: String, name: String): ApiResponse<LatestRelease> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/repos/$owner/$name/releases/latest")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Get
        }
    }

    suspend fun getGist(id: String): ApiResponse<Gist> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/gists/$id")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Get
        }
    }

    suspend fun createGist(
        description: String,
        isPublic: Boolean,
        fileName: String,
        content: String,
    ): ApiResponse<Gist> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/gists")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Post
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CreateGistBody(description, isPublic, mapOf(fileName to GistFileBody(content))))
        }
    }

    suspend fun deleteGist(id: String): ApiResponse<String> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/gists/$id")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Delete
        }
    }

    // ── Developers / commit history ──────────────────────────────────────────

    suspend fun searchUsers(query: String, perPage: Int = 30): ApiResponse<UserSearchResponse> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/search/users?q=${query.encodeURLParameter()}&sort=followers&order=desc&per_page=$perPage")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Get
            }
        }

    suspend fun getCommits(owner: String, repo: String, perPage: Int = 60): ApiResponse<List<CommitItem>> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/repos/$owner/$repo/commits?per_page=$perPage")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Get
            }
        }

    /** 52 weeks of commit counts. GitHub answers 202 (empty) while it computes the stats. */
    suspend fun getCommitActivity(owner: String, repo: String): ApiResponse<List<CommitActivityWeek>> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/repos/$owner/$repo/stats/commit_activity")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Get
            }
        }

    // ── Raw GraphQL (Discussions, Projects) ──────────────────────────────────

    @Serializable
    private data class GraphQLBody(val query: String, val variables: JsonObject)

    suspend fun graphql(
        query: String,
        variables: JsonObject = JsonObject(emptyMap()),
    ): ApiResponse<JsonObject> = withContext(Dispatchers.IO) {
        client.request {
            url("$base/graphql")
            header(HttpHeaders.Authorization, auth())
            method = HttpMethod.Post
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(GraphQLBody(query, variables))
        }
    }
}
