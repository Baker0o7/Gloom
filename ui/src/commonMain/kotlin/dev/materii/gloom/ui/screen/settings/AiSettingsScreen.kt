package dev.materii.gloom.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.settings.viewmodel.AiSettingsViewModel
import kotlinx.coroutines.launch

class AiSettingsScreen : Screen {

    @Composable
    override fun Content() = Screen()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen() {
        val viewModel: AiSettingsViewModel = koinScreenModel()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var draft by rememberSaveable { mutableStateOf(viewModel.currentKey) }
        var keyVisible by rememberSaveable { mutableStateOf(false) }

        val isSaved   = draft.trim() == viewModel.currentKey && viewModel.currentKey.isNotBlank()
        val hasChange = draft.trim() != viewModel.currentKey

        Scaffold(
            topBar = {
                LargeToolbar(title = "AI Settings", scrollBehavior = scrollBehavior)
            },
            snackbarHost = { SnackbarHost(snackbar) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { pv ->
            Column(
                modifier = Modifier
                    .padding(pv)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier)

                // Info card
                GeminiInfoCard()

                // API key input
                Text(
                    text  = "Gemini API Key",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                OutlinedTextField(
                    value         = draft,
                    onValueChange = { draft = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Paste your API key here") },
                    placeholder   = { Text("AIza…") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (keyVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon  = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Visibility toggle
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Outlined.VisibilityOff
                                                  else           Icons.Outlined.Visibility,
                                    contentDescription = if (keyVisible) "Hide key" else "Show key",
                                )
                            }
                            // Clear field
                            if (draft.isNotBlank()) {
                                IconButton(onClick = { draft = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    },
                    supportingText = {
                        when {
                            isSaved   -> Text("✓ Key saved", color = MaterialTheme.colorScheme.primary)
                            hasChange -> Text("Unsaved changes")
                            else      -> Text("No key saved — using build-time key if available")
                        }
                    }
                )

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val key = draft.trim()
                            viewModel.saveKey(key)
                            scope.launch {
                                snackbar.showSnackbar(
                                    if (key.isBlank()) "API key cleared" else "API key saved"
                                )
                            }
                        },
                        enabled  = hasChange,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Save")
                    }

                    if (viewModel.currentKey.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                draft = ""
                                viewModel.saveKey("")
                                scope.launch { snackbar.showSnackbar("API key removed") }
                            },
                        ) {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Remove")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─── Gemini info card ─────────────────────────────────────────────────────────

@Composable
private fun GeminiInfoCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text  = "Gemini 2.0 Flash — Free",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text  = "Gloom AI is powered by Google Gemini 2.0 Flash.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text  = "Free tier: 15 requests/min · 1M tokens/min · 1 500 requests/day\nNo credit card required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text  = "Get your free key at: aistudio.google.com/app/apikey",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}
