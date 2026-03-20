package dev.materii.gloom.api.repository

import dev.materii.gloom.api.service.GithubApiService
import dev.materii.gloom.api.util.getOrNull

class GithubRepository(
    private val service: GithubApiService,
) {

    suspend fun getNotifications(all: Boolean = false, page: Int = 1) =
        service.getNotifications(all = all, page = page)

    suspend fun markThreadRead(threadId: String) =
        service.markThreadRead(threadId)

    suspend fun markAllRead() =
        service.markAllRead()
}
