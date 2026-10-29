package dev.materii.gloom.ui.screen.notifications.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import dev.materii.gloom.api.dto.notification.Notification
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.api.util.ifSuccessful
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repo: GithubRepository,
) : ScreenModel {

    // This view model is a singleton shared by the nav badge and the inbox screen, so it must
    // not use screenModelScope: that scope is cancelled when the screen is disposed, which would
    // silently break every later refresh.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadJob: Job? = null

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
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            isLoading = true
            error     = null
            when (val response = repo.getNotifications()) {
                is ApiResponse.Success -> {
                    notifications.clear()
                    notifications.addAll(response.data)
                }
                is ApiResponse.Empty -> {
                    notifications.clear()
                }
                is ApiResponse.Error -> {
                    error = "Couldn't load notifications"
                }
                is ApiResponse.Failure -> {
                    error = "Network error. Please try again."
                }
            }
            isLoading = false
        }
    }

    fun markRead(id: String) {
        val idx = notifications.indexOfFirst { it.id == id }
        if (idx < 0 || !notifications[idx].unread) return
        // Optimistic update, rolled back if the request fails
        notifications[idx] = notifications[idx].copy(unread = false)
        scope.launch {
            val response = repo.markThreadRead(id)
            if (response !is ApiResponse.Success && response !is ApiResponse.Empty) {
                val i = notifications.indexOfFirst { it.id == id }
                if (i >= 0) notifications[i] = notifications[i].copy(unread = true)
            }
        }
    }

    fun markAllRead() {
        scope.launch {
            repo.markAllRead().ifSuccessful { _ ->
                val updated = notifications.map { it.copy(unread = false) }
                notifications.clear()
                notifications.addAll(updated)
            }
        }
    }
}
