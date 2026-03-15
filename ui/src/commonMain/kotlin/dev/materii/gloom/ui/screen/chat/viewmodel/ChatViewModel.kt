package dev.materii.gloom.ui.screen.chat.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.chat.ChatMessage
import dev.materii.gloom.api.repository.ChatRepository
import dev.materii.gloom.api.util.fold
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val chatRepository: ChatRepository,
) : ScreenModel {

    val messages = mutableStateListOf<ChatMessage>()

    var isThinking by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var inputText by mutableStateOf("")

    val hasApiKey: Boolean
        get() = try {
            dev.materii.gloom.api.BuildConfig.ANTHROPIC_API_KEY.isNotBlank()
        } catch (_: Exception) { false }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || isThinking) return
        inputText    = ""
        errorMessage = null

        // Snapshot history BEFORE appending the new user message
        val historySnapshot = messages.toList()
        messages.add(ChatMessage(UUID.randomUUID().toString(), ChatMessage.Role.USER, text))

        screenModelScope.launch {
            isThinking = true
            chatRepository.sendMessage(historySnapshot, text).fold(
                success = { reply ->
                    if (reply.isNotBlank()) {
                        messages.add(
                            ChatMessage(UUID.randomUUID().toString(), ChatMessage.Role.ASSISTANT, reply)
                        )
                    }
                },
                empty   = {},
                error   = { errorMessage = "Network error. Please try again." },
                failure = { errorMessage = "Failed to reach AI. Check your connection." },
            )
            isThinking = false
        }
    }

    fun clearConversation() {
        messages.clear()
        errorMessage = null
        chatRepository.context = ChatRepository.GitHubContext()
    }

    fun setContext(context: ChatRepository.GitHubContext) {
        chatRepository.context = context
    }
}
