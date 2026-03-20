package dev.materii.gloom.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
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
import dev.materii.gloom.api.service.ai.AIService
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.settings.viewmodel.AISettingsViewModel
import kotlinx.coroutines.launch

class AISettingsScreen : Screen {

    @Composable
    override fun Content() = Screen()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen() {
        val viewModel     = koinScreenModel<AISettingsViewModel>()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val snackbar       = remember { SnackbarHostState() }
        val scope          = rememberCoroutineScope()

        var apiKey     by rememberSaveable { mutableStateOf(viewModel.apiKey) }
        var apiUrl     by rememberSaveable { mutableStateOf(viewModel.apiUrl) }
        var aiEnabled  by rememberSaveable { mutableStateOf(viewModel.aiEnabled) }
        var keyVisible by rememberSaveable { mutableStateOf(false) }
        var isTesting  by remember { mutableStateOf(false) }
        var testResult by remember { mutableStateOf<Boolean?>(null) }

        Scaffold(
            topBar = {
                LargeToolbar(
                    title          = "AI Settings",
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = {
                            viewModel.saveSettings(apiKey, apiUrl, aiEnabled)
                            scope.launch { snackbar.showSnackbar("Settings saved") }
                        }) {
                            Icon(Icons.Outlined.Check, contentDescription = "Save")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            modifier     = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { pv ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier)

                // ── Info card ─────────────────────────────────────────────────
                ZAiInfoCard()

                // ── Enable toggle ─────────────────────────────────────────────
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable AI Assistant", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Show AI tab and enable chat features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = aiEnabled, onCheckedChange = { aiEnabled = it })
                    }
                }

                // ── API Key ───────────────────────────────────────────────────
                Text("API Key", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value          = apiKey,
                    onValueChange  = { apiKey = it; testResult = null },
                    modifier       = Modifier.fillMaxWidth(),
                    label          = { Text("Z.AI API Key") },
                    placeholder    = { Text("zai-…") },
                    singleLine     = true,
                    shape          = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (keyVisible) VisualTransformation.None
                                           else           PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (keyVisible) "Hide" else "Show",
                                )
                            }
                            if (apiKey.isNotBlank()) {
                                IconButton(onClick = { apiKey = ""; testResult = null }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    },
                    supportingText = {
                        Text(
                            if (apiKey.trim() == viewModel.apiKey && apiKey.isNotBlank())
                                "✓ Key saved"
                            else if (apiKey.isBlank()) "Get a key at z.ai"
                            else "Unsaved"
                        )
                    }
                )

                // ── Custom URL ────────────────────────────────────────────────
                Text("API URL", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value         = apiUrl,
                    onValueChange = { apiUrl = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Custom base URL") },
                    placeholder   = { Text(AIService.DEFAULT_BASE_URL) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText  = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Leave empty to use ${AIService.DEFAULT_BASE_URL}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                )

                // ── Test + Save ───────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isTesting  = true
                                testResult = null
                                testResult = viewModel.testConnection(apiKey, apiUrl)
                                isTesting  = false
                            }
                        },
                        enabled  = apiKey.isNotBlank() && !isTesting,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Testing…")
                        } else {
                            Text("Test Connection")
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveSettings(apiKey, apiUrl, aiEnabled)
                            scope.launch { snackbar.showSnackbar("Settings saved") }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }

                // ── Test result ───────────────────────────────────────────────
                testResult?.let { ok ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer
                                             else     MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier          = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (ok) Icons.Outlined.Check else Icons.Outlined.Close,
                                contentDescription = null,
                                tint = if (ok) MaterialTheme.colorScheme.onPrimaryContainer
                                       else     MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                if (ok) "Connected to Z.AI successfully!"
                                else    "Connection failed. Check your key and network.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (ok) MaterialTheme.colorScheme.onPrimaryContainer
                                        else     MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─── Z.AI info card ───────────────────────────────────────────────────────────

@Composable
private fun ZAiInfoCard() {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Z.AI — Smart Coding Assistant",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                "Gloom AI is powered by Z.AI, a capable assistant optimised for code understanding, generation, and GitHub workflows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Get your API key at: z.ai",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}
