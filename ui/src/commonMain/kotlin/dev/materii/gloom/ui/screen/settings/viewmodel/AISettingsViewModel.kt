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

    val isSignedIn: Boolean get() = aiService.isSignedIn()
    val savedEmail: String  get() = prefs.aiEmail
    val aiEnabled: Boolean  get() = prefs.aiEnabled

    /** Calls chat.z.ai/api/v1/auths/signin and stores the session token. */
    suspend fun signIn(email: String, password: String): Result<String> =
        aiService.signIn(email, password)

    fun signOut() = aiService.signOut()

    fun setAiEnabled(enabled: Boolean) {
        prefs.aiEnabled = enabled
    }
}
