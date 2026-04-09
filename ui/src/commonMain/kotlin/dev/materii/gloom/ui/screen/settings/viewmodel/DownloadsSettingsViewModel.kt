package dev.materii.gloom.ui.screen.settings.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import dev.materii.gloom.domain.manager.PreferenceManager

class DownloadsSettingsViewModel(
    val prefs: PreferenceManager
) : ScreenModel
