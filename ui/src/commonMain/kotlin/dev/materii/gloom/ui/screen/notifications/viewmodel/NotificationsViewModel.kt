package dev.materii.gloom.ui.screen.notifications.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.notification.NotificationDto
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ifSuccessful
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repo: GithubRepository
) : ScreenModel {

    val notifications = mutableStateListOf<NotificationDto>()

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
            repo.getNotifications(all = true).ifSuccessful { items ->
                notifications.clear()
                notifications.addAll(items)
            }
            // If still empty after load and no items came back, surface the error state
            if (notifications.isEmpty()) hasError = false // empty inbox is valid
            isLoading = false
        }
    }

    fun markRead(threadId: String) {
        screenModelScope.launch {
            repo.markThreadRead(threadId).ifSuccessful {
                val index = notifications.indexOfFirst { it.id == threadId }
                if (index != -1) {
                    notifications[index] = notifications[index].copy(unread = false)
                }
            }
        }
    }

    fun markAllRead() {
        screenModelScope.launch {
            repo.markAllRead().ifSuccessful {
                val updated = notifications.map { it.copy(unread = false) }
                notifications.clear()
                notifications.addAll(updated)
            }
        }
    }
}
