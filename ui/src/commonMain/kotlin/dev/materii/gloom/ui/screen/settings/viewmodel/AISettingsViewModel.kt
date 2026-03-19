package dev.materii.gloom.ui.screen.settings.viewmodel

import dev.materii.gloom.domain.manager.PreferenceManager
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AISettingsViewModel : KoinComponent {

    private val prefs: PreferenceManager by inject()
    private val httpClient: HttpClient by inject()

    val apiUrl: String get() = prefs.aiApiUrl
    val aiEnabled: Boolean get() = prefs.aiEnabled

    fun saveSettings(url: String, enabled: Boolean) {
        prefs.aiApiUrl = url.trim()
        prefs.aiEnabled = enabled
    }

    suspend fun testConnection(url: String): Boolean = withContext(Dispatchers.IO) {
        val testUrl = url.trim().ifEmpty { "http://10.0.2.2:3001" }
        return@withContext try {
            val response = httpClient.get("$testUrl/api/health")
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
