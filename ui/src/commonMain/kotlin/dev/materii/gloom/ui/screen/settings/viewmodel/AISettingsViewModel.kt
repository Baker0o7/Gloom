package dev.materii.gloom.ui.screen.settings.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.domain.manager.PreferenceManager
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.util.ApiResponse
import kotlinx.coroutines.launch

class AISettingsViewModel(
    private val prefs: PreferenceManager,
    private val aiService: AIService,
) : ScreenModel {

    val apiKey: String   get() = prefs.aiApiKey
    val apiUrl: String   get() = prefs.aiApiUrl
    val aiEnabled: Boolean get() = prefs.aiEnabled

    fun saveSettings(key: String, url: String, enabled: Boolean) {
        prefs.aiApiKey  = key.trim()
        prefs.aiApiUrl  = url.trim()
        prefs.aiEnabled = enabled
    }

    /** Returns true if z.ai responds with 2xx to a real (empty) chat request. */
    suspend fun testConnection(key: String, url: String): Boolean {
        // Temporarily override prefs so the service uses the draft values
        val oldKey = prefs.aiApiKey
        val oldUrl = prefs.aiApiUrl
        prefs.aiApiKey = key.trim()
        prefs.aiApiUrl = url.trim()

        val result = aiService.chat(
            messages  = listOf(ChatMessage("user", "ping")),
            maxTokens = 10,
        )

        prefs.aiApiKey = oldKey
        prefs.aiApiUrl = oldUrl
        return result is ApiResponse.Success
    }
}
