package dev.materii.gloom.ui.screen.chat

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.materii.gloom.api.dto.chat.ChatMessage
import dev.materii.gloom.api.dto.chat.MessageSegment
import dev.materii.gloom.api.dto.chat.parseSegments
import dev.materii.gloom.ui.screen.chat.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ─── Public FAB entry point ───────────────────────────────────────────────────

@Composable
fun ChatFab(modifier: Modifier = Modifier) {
    val viewModel: ChatViewModel = koinInject()
    var sheetOpen by remember { mutableStateOf(false) }

    val fabRotation by animateFloatAsState(
        targetValue = if (viewModel.isThinking) 360f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "fab_rot"
    )

    FloatingActionButton(
        onClick = { sheetOpen = true },
        shape          = CircleShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier       = modifier,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = "Open AI Chat",
            modifier = Modifier.rotate(fabRotation),
        )
    }

    if (sheetOpen) {
        ChatSheet(viewModel = viewModel, onDismiss = { sheetOpen = false })
    }
}

// ─── Bottom sheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSheet(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    fun close() = scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle       = null,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            ChatHeader(
                messageCount = viewModel.messages.size,
                onClear      = viewModel::clearConversation,
                onClose      = ::close,
            )

            // Message list
            MessageList(
                messages   = viewModel.messages,
                isThinking = viewModel.isThinking,
                snackbar   = snackbarHost,
                modifier   = Modifier.weight(1f),
            )

            // Error
            viewModel.errorMessage?.let { ErrorBanner(it) }

            // Input
            InputRow(
                text         = viewModel.inputText,
                onTextChange = { viewModel.inputText = it },
                onSend       = viewModel::sendMessage,
                enabled      = viewModel.hasApiKey && !viewModel.isThinking,
            )
        }

        SnackbarHost(snackbarHost)
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(messageCount: Int, onClear: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.SmartToy,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp).size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Gloom AI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Gemini 2.0 Flash · Free · Android assistant",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (messageCount > 0) {
            IconButton(onClick = onClear) {
                Icon(Icons.Outlined.Delete, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Message list ─────────────────────────────────────────────────────────────

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LazyColumn(
        state          = listState,
        modifier       = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (messages.isEmpty() && !isThinking) item { WelcomeMessage() }

        items(messages, key = { it.id }) { msg ->
            MessageBubble(message = msg, snackbar = snackbar)
        }

        if (isThinking) item { ThinkingBubble() }
    }
}

// ─── Welcome ──────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Text("Gloom AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Free Android & Kotlin assistant.\nAsk about code, repos, or anything GitHub.",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            "Explain this file",
            "Write a Compose LazyColumn",
            "How do I use Koin in KMP?",
            "Review open issues",
        ).forEach { SuggestionChip(it) }
    }
}

@Composable
private fun SuggestionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

// ─── Message bubble ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessage, snackbar: SnackbarHostState) {
    val isUser = message.role == ChatMessage.Role.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        if (isUser) {
            // User messages: single bubble
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp, topEnd = 4.dp,
                    bottomStart = 18.dp, bottomEnd = 18.dp
                ),
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Text(
                    text     = message.content,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        } else {
            // AI messages: parse into text + code segments
            val segments = remember(message.id) { parseSegments(message.content) }
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                segments.forEach { segment ->
                    when (segment) {
                        is MessageSegment.Text -> AssistantTextBubble(segment.content)
                        is MessageSegment.Code -> CodeBlock(
                            language = segment.language,
                            code     = segment.code,
                            snackbar = snackbar,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantTextBubble(text: String) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 4.dp, topEnd = 18.dp,
            bottomStart = 18.dp, bottomEnd = 18.dp
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

// ─── Code block with language label + copy button ─────────────────────────────

@Composable
private fun CodeBlock(language: String, code: String, snackbar: SnackbarHostState) {
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E2E)),  // dark editor background regardless of theme
    ) {
        // Language bar + copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF313244))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = language.ifBlank { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCDD6F4),
                fontFamily = FontFamily.Monospace,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        scope.launch { snackbar.showSnackbar("Copied") }
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = "Copy code",
                    tint     = Color(0xFFCDD6F4),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    "copy",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Color(0xFFCDD6F4),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // Scrollable code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text       = code,
                style      = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize   = 12.sp,
                color      = Color(0xFFCDD6F4),
                softWrap   = false,  // prevent wrapping — scroll horizontally instead
            )
        }
    }
}

// ─── Thinking indicator ───────────────────────────────────────────────────────

@Composable
private fun ThinkingBubble() {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(14.dp),
                    strokeCap   = StrokeCap.Round,
                    strokeWidth = 2.dp,
                )
                Text(
                    "Thinking…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Input row ────────────────────────────────────────────────────────────────

@Composable
private fun InputRow(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value        = text,
            onValueChange = onTextChange,
            modifier     = Modifier.weight(1f),
            placeholder  = {
                Text(
                    "Ask about code, repos, Kotlin…",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            enabled  = enabled,
            maxLines = 5,
            shape    = RoundedCornerShape(24.dp),
            colors   = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction      = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        FilledIconButton(
            onClick  = onSend,
            enabled  = enabled && text.isNotBlank(),
            modifier = Modifier.size(48.dp),
            shape    = CircleShape,
        ) {
            Icon(Icons.Outlined.Send, "Send")
        }
    }
}

// ─── Error banner ─────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = message,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
