package dev.materii.gloom.ui.screen.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.materii.gloom.api.dto.ai.ChatMessage
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class AICommitMessageScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val aiService  = koinInject<AIService>()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val snackbar   = remember { SnackbarHostState() }
        val scope      = rememberCoroutineScope()
        val clipboard  = LocalClipboardManager.current

        var description by remember { mutableStateOf("") }
        var result      by remember { mutableStateOf("") }
        var isLoading   by remember { mutableStateOf(false) }

        Scaffold(
            topBar = { LargeToolbar(title = "Commit Message Generator", scrollBehavior = scrollBehavior) },
            snackbarHost = { SnackbarHost(snackbar) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { pv ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Describe what you changed and get a conventional commit message.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    modifier      = Modifier.fillMaxWidth().height(140.dp),
                    label         = { Text("What did you change?") },
                    placeholder   = { Text("e.g. Fixed crash when opening profile with no avatar, added null check") },
                )

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            result    = ""
                            val messages = listOf(
                                ChatMessage("system", "You are an expert at writing concise, conventional Git commit messages. Follow the format: <type>(<scope>): <description>. Types: feat, fix, docs, style, refactor, test, chore. Keep subject under 72 chars. Optionally add a body after a blank line."),
                                ChatMessage("user", "Write a conventional commit message for: $description\n\nProvide ONLY the commit message, no explanation.")
                            )
                            val res = aiService.chat(messages = messages, maxTokens = 256)
                            if (res is ApiResponse.Success) {
                                result = res.data.choices.firstOrNull()?.message?.content?.trim() ?: ""
                            }
                            isLoading = false
                        }
                    },
                    enabled  = description.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Generate Commit Message")
                }

                if (result.isNotBlank()) {
                    Surface(
                        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = MaterialTheme.shapes.medium,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Text("Result", style = MaterialTheme.typography.labelLarge)
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(result))
                                    scope.launch { snackbar.showSnackbar("Copied to clipboard") }
                                }) {
                                    Icon(Icons.Outlined.ContentCopy, "Copy")
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                result,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}
