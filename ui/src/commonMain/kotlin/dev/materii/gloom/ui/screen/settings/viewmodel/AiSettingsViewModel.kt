package dev.materii.gloom.ui.screen.settings.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import dev.materii.gloom.domain.manager.PreferenceManager

class AiSettingsViewModel(
    private val prefs: PreferenceManager,
) : ScreenModel {

    /** The currently persisted key (empty string if none). */
    val currentKey: String
        get() = prefs.geminiApiKey

    fun saveKey(key: String) {
        prefs.geminiApiKey = key.trim()
    }
}
