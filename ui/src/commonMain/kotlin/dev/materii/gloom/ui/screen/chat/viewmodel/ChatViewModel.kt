package dev.materii.gloom.ui.screen.chat.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.benasher44.uuid.uuid4
import dev.materii.gloom.api.dto.chat.ChatMessage
import dev.materii.gloom.api.repository.ChatRepository
import dev.materii.gloom.api.util.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Plain class (not Voyager ScreenModel) so it can be a Koin singleton,
 * keeping conversation alive across FAB open/close sessions.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val messages = mutableStateListOf<ChatMessage>()

    var isThinking by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var inputText by mutableStateOf("")

    val hasApiKey: Boolean
        get() = chatRepository.hasApiKey

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || isThinking) return
        inputText    = ""
        errorMessage = null

        // Snapshot history BEFORE appending the new user message
        val historySnapshot = messages.toList()
        messages.add(ChatMessage(uuid4().toString(), ChatMessage.Role.USER, text))

        scope.launch {
            isThinking = true
            chatRepository.sendMessage(historySnapshot, text).fold(
                success = { reply ->
                    if (reply.isNotBlank()) {
                        messages.add(
                            ChatMessage(uuid4().toString(), ChatMessage.Role.ASSISTANT, reply)
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

    fun dispose() {
        scope.cancel()
    }
}
