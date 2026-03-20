package dev.materii.gloom.ui.screen.notifications.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.notification.Notification
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ifSuccessful
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repo: GithubRepository,
) : ScreenModel {

    val notifications = mutableStateListOf<Notification>()
    val unreadCount: Int get() = notifications.count { it.unread }

    var filterReason by mutableStateOf<String?>(null)

    val filteredNotifications: List<Notification>
        get() = if (filterReason == null) notifications
                else notifications.filter { it.reason == filterReason }

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init { load() }

    fun load() {
        screenModelScope.launch {
            isLoading = true
            error     = null
            repo.getNotifications().ifSuccessful { list ->
                notifications.clear()
                notifications.addAll(list)
            }
            if (notifications.isEmpty()) error = "Couldn't load notifications"
            isLoading = false
        }
    }

    fun markRead(id: String) {
        screenModelScope.launch {
            repo.markThreadRead(id).ifSuccessful { _ ->
                val idx = notifications.indexOfFirst { it.id == id }
                if (idx >= 0) notifications[idx] = notifications[idx].copy(unread = false)
            }
        }
    }

    fun markAllRead() {
        screenModelScope.launch {
            repo.markAllRead().ifSuccessful { _ ->
                val updated = notifications.map { it.copy(unread = false) }
                notifications.clear()
                notifications.addAll(updated)
            }
        }
    }
}
