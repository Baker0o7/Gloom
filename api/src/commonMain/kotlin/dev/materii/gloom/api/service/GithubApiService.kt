package dev.materii.gloom.api.service

import dev.materii.gloom.api.dto.notification.Notification
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            contentType(ContentType.Application.Json)
            setBody(CreateIssueBody(title, body))
        }
    }
}
