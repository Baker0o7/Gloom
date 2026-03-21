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
import androidx.compose.material.icons.outlined.Clear
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
import androidx.compose.material3.OutlinedButton
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
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.settings.viewmodel.AISettingsViewModel
import kotlinx.coroutines.launch

class AISettingsScreen : Screen {

    @Composable
    override fun Content() = Screen()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen() {
        val viewModel      = koinScreenModel<AISettingsViewModel>()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val snackbar       = remember { SnackbarHostState() }
        val scope          = rememberCoroutineScope()

        var apiKey      by rememberSaveable { mutableStateOf(viewModel.apiKey) }
        var keyVisible  by rememberSaveable { mutableStateOf(false) }
        var isTesting   by remember { mutableStateOf(false) }
        var testOk      by remember { mutableStateOf<Boolean?>(null) }
        var aiEnabled   by rememberSaveable { mutableStateOf(viewModel.aiEnabled) }

        val isSaved   = apiKey.trim() == viewModel.apiKey && viewModel.apiKey.isNotBlank()
        val hasChange = apiKey.trim() != viewModel.apiKey

        Scaffold(
            topBar = { LargeToolbar(title = "AI Settings", scrollBehavior = scrollBehavior) },
            snackbarHost = { SnackbarHost(snackbar) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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

                // Info card
                InfoCard()

                // Enable toggle
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable AI Assistant", style = MaterialTheme.typography.titleMedium)
                            Text("Show AI tab and chat features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = aiEnabled, onCheckedChange = {
                            aiEnabled = it
                            viewModel.setAiEnabled(it)
                        })
                    }
                }

                // Step-by-step instructions
                StepsCard()

                // API key field
                Text("API Key", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value         = apiKey,
                    onValueChange = { apiKey = it; testOk = null },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Paste your Z.AI API key") },
                    placeholder   = { Text("e.g. abc123...") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (keyVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (keyVisible) "Hide" else "Show",
                                )
                            }
                            if (apiKey.isNotBlank()) {
                                IconButton(onClick = { apiKey = ""; testOk = null }) {
                                    Icon(Icons.Outlined.Clear, "Clear")
                                }
                            }
                        }
                    },
                    supportingText = {
                        when {
                            isSaved   -> Text("✓ Key saved", color = MaterialTheme.colorScheme.primary)
                            hasChange -> Text("Unsaved changes")
                            else      -> Text("No key saved")
                        }
                    }
                )

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveKey(apiKey)
                            scope.launch { snackbar.showSnackbar("API key saved") }
                        },
                        enabled  = hasChange && apiKey.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                testOk    = null
                                testOk    = viewModel.testConnection(apiKey)
                                isTesting = false
                            }
                        },
                        enabled  = apiKey.isNotBlank() && !isTesting,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Testing…")
                        } else {
                            Text("Test")
                        }
                    }
                }

                // Test result
                testOk?.let { ok ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer
                                             else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            if (ok) "✓ Connected to Z.AI successfully!"
                            else    "✗ Connection failed — check your key and try again.",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = if (ok) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.AutoAwesome, null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp))
            Column {
                Text("Z.AI — GLM Smart Assistant",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Free tier includes GLM-4 Flash (fast & capable)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun StepsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("How to get your API key",
                style = MaterialTheme.typography.titleSmall)
            Text(
                "1. Open z.ai in your browser\n" +
                "2. Sign up or log in to your account\n" +
                "3. Go to Settings → API Keys\n" +
                "4. Click \"Create API Key\" and copy it\n" +
                "5. Paste it in the field below and tap Save",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
