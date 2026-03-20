package dev.materii.gloom.ui.screen.ai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import kotlinx.coroutines.launch

class AIViewModel(
    private val aiService: AIService,
    private val authManager: AuthManager
) : ScreenModel {

    // Chat messages
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages.toList()

    // Selected model
    var selectedModel by mutableStateOf(AIService.AVAILABLE_MODELS.first())
        private set

    // UI State
    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var inputText by mutableStateOf("")
        private set

    // Available models
    val availableModels: List<AIService.ModelInfo> get() = aiService.getAvailableModels()

    // Check if user is authenticated
    val isAuthenticated: Boolean get() = authManager.isSignedIn

    // Check if API key is configured
    val hasApiKey: Boolean get() = aiService.hasApiKey()

    init {
        // Add system message for coding assistant context
        _messages.add(aiService.createCodingSystemMessage())
    }

    fun onInputChange(text: String) {
        inputText = text
    }

    fun selectModel(model: AIService.ModelInfo) {
        selectedModel = model
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || isLoading) return

        inputText = ""
        error = null

        // Add user message
        val userMessage = aiService.createUserMessage(text)
        _messages.add(userMessage)

        isLoading = true

        screenModelScope.launch {
            // Prepare messages for API
            val apiMessages = _messages.toList()

            val result = aiService.chat(
                messages = apiMessages,
                model = selectedModel.id,
                temperature = 0.7,
                maxTokens = 4096
            )

            isLoading = false

            when (result) {
                is ApiResponse.Success -> {
                    val assistantMessage = result.data.choices.firstOrNull()?.message
                    if (assistantMessage != null) {
                        _messages.add(assistantMessage)
                    } else {
                        error = "No response received from AI. Please try again."
                    }
                }
                is ApiResponse.Error -> {
                    val errorMsg = result.error.message ?: "Unknown error"
                    error = when {
                        errorMsg.contains("401", ignoreCase = true) -> "Z.AI key rejected (401). Your key may be invalid or expired. Open Settings → AI Settings to update it."
                        errorMsg.contains("403", ignoreCase = true) -> "Z.AI access denied (403). Check your API key in Settings → AI Settings."
                        errorMsg.contains("404", ignoreCase = true) -> "Z.AI endpoint not found (404). Check the Custom URL in Settings → AI Settings."
                        errorMsg.contains("429", ignoreCase = true) -> "Rate limit exceeded. Please wait a moment and try again."
                        else -> "Error: $errorMsg"
                    }
                }
                is ApiResponse.Failure -> {
                    val exception = result.error
                    error = when {
                        exception.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                            "No internet connection. Please check your network."
                        exception.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Request timed out. Please try again."
                        exception.message?.contains("SSL", ignoreCase = true) == true -> 
                            "SSL error. Please check your connection."
                        else -> "Network error: ${exception.message ?: "Unknown error"}"
                    }
                }
                is ApiResponse.Empty -> {
                    error = "Empty response received from server."
                }
            }
        }
    }

    fun clearChat() {
        _messages.clear()
        _messages.add(aiService.createCodingSystemMessage())
        error = null
    }

    fun dismissError() {
        error = null
    }
}
