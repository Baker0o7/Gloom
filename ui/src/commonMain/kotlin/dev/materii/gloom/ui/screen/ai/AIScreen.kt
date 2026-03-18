package dev.materii.gloom.ui.screen.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.ai.viewmodel.AIViewModel
import kotlinx.coroutines.launch

class AIScreen : Tab {

    override val options: TabOptions
        @Composable get() {
            val navigator = LocalTabNavigator.current
            val selected = navigator.current == this
            return TabOptions(
                0u,
                stringResource(Res.strings.navigation_ai),
                rememberVectorPainter(if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome)
            )
        }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun Content() {
        val viewModel: AIViewModel = koinScreenModel()
        val scrollState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var showModelSelector by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                LargeToolbar(
                    title = stringResource(Res.strings.ai_title),
                    actions = {
                        IconButton(onClick = { showModelSelector = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(Res.strings.ai_select_model)
                            )
                        }
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(Res.strings.ai_clear_chat)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (!viewModel.isAuthenticated) {
                // Not authenticated state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(Res.strings.ai_not_authenticated),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Model indicator
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = viewModel.selectedModel.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Chat messages
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = scrollState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Welcome message
                        item {
                            WelcomeMessage()
                        }

                        // Chat messages (skip system message at index 0)
                        items(
                            items = viewModel.messages.drop(1),
                            key = { it.content + it.role }
                        ) { message ->
                            MessageBubble(message = message)
                        }

                        // Loading indicator
                        if (viewModel.isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = stringResource(Res.strings.ai_thinking),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Error message
                    AnimatedVisibility(
                        visible = viewModel.error != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = viewModel.error ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.dismissError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Input area
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.inputText,
                                onValueChange = { viewModel.onInputChange(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(stringResource(Res.strings.ai_hint)) },
                                maxLines = 5,
                                shape = RoundedCornerShape(24.dp)
                            )

                            FilledIconButton(
                                onClick = {
                                    viewModel.sendMessage()
                                    coroutineScope.launch {
                                        scrollState.animateScrollToItem(Int.MAX_VALUE)
                                    }
                                },
                                enabled = viewModel.inputText.isNotBlank() && !viewModel.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = stringResource(Res.strings.ai_send)
                                )
                            }
                        }
                    }
                }
            }

            // Model selector dialog
            if (showModelSelector) {
                ModelSelectorDialog(
                    models = viewModel.availableModels,
                    selectedModel = viewModel.selectedModel,
                    onModelSelected = { model ->
                        viewModel.selectModel(model)
                        showModelSelector = false
                    },
                    onDismiss = { showModelSelector = false }
                )
            }
        }
    }

    @Composable
    private fun WelcomeMessage() {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Gloom AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = stringResource(Res.strings.ai_welcome),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    @Composable
    private fun MessageBubble(message: ChatMessage) {
        val isUser = message.role == "user"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                color = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Role label
                    Text(
                        text = if (isUser) "You" else "Assistant",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Message content
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    @Composable
    private fun ModelSelectorDialog(
        models: List<AIService.ModelInfo>,
        selectedModel: AIService.ModelInfo,
        onModelSelected: (AIService.ModelInfo) -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.strings.ai_select_model)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    models.forEach { model ->
                        FilterChip(
                            selected = model.id == selectedModel.id,
                            onClick = { onModelSelected(model) },
                            label = {
                                Column {
                                    Text(
                                        text = model.displayName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = model.description,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        )
    }
}
