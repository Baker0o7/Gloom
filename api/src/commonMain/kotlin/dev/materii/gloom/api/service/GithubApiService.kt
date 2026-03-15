package dev.materii.gloom.api.service

import dev.materii.gloom.api.URLs
import dev.materii.gloom.api.dto.notification.NotificationDto
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
    private val authManager: AuthManager
) {

    private fun authHeader() = "Bearer ${authManager.authToken}"

    suspend fun getNotifications(
        all: Boolean = true,
        page: Int = 1,
        perPage: Int = 50
    ): ApiResponse<List<NotificationDto>> =
        withContext(Dispatchers.IO) {
            client.request {
                url("${URLs.BASE_URL}/notifications?all=$all&page=$page&per_page=$perPage")
                header(HttpHeaders.Authorization, authHeader())
                method = HttpMethod.Get
            }
        }

    suspend fun markThreadRead(threadId: String): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url("${URLs.BASE_URL}/notifications/threads/$threadId")
                header(HttpHeaders.Authorization, authHeader())
                method = HttpMethod.Patch
            }
        }

    suspend fun markAllRead(): ApiResponse<String> =
        withContext(Dispatchers.IO) {
            client.request {
                url("${URLs.BASE_URL}/notifications")
                header(HttpHeaders.Authorization, authHeader())
                method = HttpMethod.Put
            }
        }
}