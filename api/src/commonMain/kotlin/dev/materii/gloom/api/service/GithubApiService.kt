package dev.materii.gloom.api.service

import dev.materii.gloom.api.dto.notification.Notification
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /** Mark a single thread as read (PATCH → 205 No Content). */
    suspend fun markThreadRead(threadId: String): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/notifications/threads/$threadId")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Patch
            }
        }

    /** Mark all notifications as read (PUT → 202 or 205). */
    suspend fun markAllRead(): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url("$base/notifications")
                header(HttpHeaders.Authorization, auth())
                method = HttpMethod.Put
            }
        }
}
