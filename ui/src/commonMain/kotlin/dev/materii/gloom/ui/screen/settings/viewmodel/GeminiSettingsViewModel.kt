package dev.materii.gloom.ui.screen.settings.viewmodel

import dev.materii.gloom.domain.manager.PreferenceManager

class GeminiSettingsViewModel(
    private val prefs: PreferenceManager
) {

    val apiKey: String
        get() = prefs.geminiApiKey

    fun saveApiKey(key: String) {
        prefs.geminiApiKey = key
    }

    fun clearApiKey() {
        prefs.geminiApiKey = ""
    }

}
