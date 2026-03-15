package dev.materii.gloom.api.repository

import dev.materii.gloom.api.dto.notification.NotificationDto
import dev.materii.gloom.api.service.GithubApiService
import dev.materii.gloom.api.util.ApiResponse

class GithubRepository(
    private val service: GithubApiService
) {

    suspend fun getNotifications(
        all: Boolean = true,
        page: Int = 1,
        perPage: Int = 50
    ): ApiResponse<List<NotificationDto>> =
        service.getNotifications(all, page, perPage)

    suspend fun markThreadRead(threadId: String): ApiResponse<String> =
        service.markThreadRead(threadId)

    suspend fun markAllRead(): ApiResponse<String> =
        service.markAllRead()

}