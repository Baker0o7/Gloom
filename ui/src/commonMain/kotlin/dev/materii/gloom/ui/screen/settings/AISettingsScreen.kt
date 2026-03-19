package dev.materii.gloom.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.settings.viewmodel.AISettingsViewModel
import kotlinx.coroutines.launch

class AISettingsScreen : Screen {

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun Content() {
        val viewModel: AISettingsViewModel = koinScreenModel()
        val navigator = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var apiUrl by remember { mutableStateOf(viewModel.apiUrl) }
        var aiEnabled by remember { mutableStateOf(viewModel.aiEnabled) }
        var testResult by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            apiUrl = viewModel.apiUrl
            aiEnabled = viewModel.aiEnabled
        }

        Scaffold(
            topBar = {
                LargeToolbar(
                    title = "AI Settings",
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.saveSettings(apiUrl, aiEnabled)
                            navigator.pop()
                        }) {
                            Icon(Icons.Outlined.Check, contentDescription = "Save")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier)

                // AI Enable Toggle
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable AI Assistant",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Show AI floating button and enable chat features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = aiEnabled,
                            onCheckedChange = { aiEnabled = it }
                        )
                    }
                }

                // API URL Setting
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "API URL",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiUrl,
                            onValueChange = { apiUrl = it },
                            label = { Text("Custom API URL") },
                            placeholder = { Text("http://10.0.2.2:3001") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Leave empty for default (Android emulator: 10.0.2.2:3001)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Test Connection Button
                Button(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testResult = null
                            val result = viewModel.testConnection(apiUrl)
                            isTesting = false
                            testResult = if (result) {
                                "Connection successful!"
                            } else {
                                "Connection failed. Check if backend is running."
                            }
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Text("Test Connection")
                    }
                }

                // Test Result
                testResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.contains("successful"))
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (result.contains("successful")) Icons.Outlined.Check else Icons.Outlined.Close,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(result)
                        }
                    }
                }

                // Help Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Setup Instructions",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = """
                                1. Start the AI backend server:
                                   cd ai-backend && npm run dev

                                2. For Android emulator:
                                   Leave URL empty (uses 10.0.2.2:3001)

                                3. For real device:
                                   Set URL to your PC's IP address
                                   Example: http://192.168.1.100:3001

                                4. Make sure port 3001 is accessible
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
