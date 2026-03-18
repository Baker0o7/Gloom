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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: AIViewModel = koinScreenModel()
        val scrollState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val canScrollBackToTop by remember { derivedStateOf { lazyListState.canScrollBackward } }

        Scaffold(
            topBar = {                LargeToolbar(
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
        ) { pv ->
            if (!viewModel.isAuthenticated) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pv),
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
                        .padding(pv)
                ) {
                    // Model indicator
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical)
.lex, "8.dp)
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
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = scrollState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                        reverseLayout = true
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(viewModel.messages.size)
                        }
                    )

                    // Chat messages
                    items(
                        messages = viewModel.messages.drop(1), // Skip system message
                        key = { it.content.hashCode() + it.role.hashCode() }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isUser = message.role == "user"
                        )
                    }
                }
            }
        }
    }
}