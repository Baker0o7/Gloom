package dev.materii.gloom.ui.screen.settings.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.domain.manager.PreferenceManager
import kotlinx.coroutines.launch

class AISettingsViewModel(
    private val prefs: PreferenceManager,
    private val aiService: AIService,
) : ScreenModel {

    val apiKey: String     get() = prefs.aiApiKey
    val aiEnabled: Boolean get() = prefs.aiEnabled

    fun saveKey(key: String) {
        prefs.aiApiKey = key.trim()
    }

    fun setAiEnabled(enabled: Boolean) {
        prefs.aiEnabled = enabled
    }

    /** Sends a minimal test message to verify the key works. */
    suspend fun testConnection(key: String): Boolean {
        val old = prefs.aiApiKey
        prefs.aiApiKey = key.trim()
        val result = aiService.chat(
            messages  = listOf(dev.materii.gloom.api.dto.ai.ChatMessage("user", "hi")),
            maxTokens = 10,
        )
        prefs.aiApiKey = old
        return result is dev.materii.gloom.api.util.ApiResponse.Success
    }
}
