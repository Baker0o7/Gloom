package dev.materii.gloom.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.settings.component.SettingsGroup
import dev.materii.gloom.ui.screen.settings.component.SettingsHeader
import dev.materii.gloom.ui.screen.settings.component.SettingsSwitch
import dev.materii.gloom.ui.screen.settings.viewmodel.AISettingsViewModel
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

class AISettingsScreen : Screen {

    @Composable
    override fun Content() = Screen()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen(
        viewModel: AISettingsViewModel = koinScreenModel()
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val coroutineScope = rememberCoroutineScope()
        var testResult by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }
        var apiUrl by remember { mutableStateOf(viewModel.prefs.aiApiUrl) }
        
        // Pre-fetch strings for use in coroutine
        val successMsg = stringResource(Res.strings.ai_settings_test_success)
        val failedMsg = stringResource(Res.strings.ai_settings_test_failed)

        Scaffold(
            topBar = { Toolbar(scrollBehavior) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { pv ->
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(pv)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsHeader(stringResource(Res.strings.settings_ai))
                
                SettingsGroup {
                    SettingsSwitch(
                        label = stringResource(Res.strings.ai_settings_enabled),
                        secondaryLabel = stringResource(Res.strings.ai_settings_enabled_description),
                        pref = viewModel.prefs.aiEnabled,
                        onPrefChange = { viewModel.prefs.aiEnabled = it }
                    )
                }

                SettingsHeader(stringResource(Res.strings.ai_settings_api_url))
                
                SettingsGroup {
                    // API URL Input
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.strings.ai_settings_api_url_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = apiUrl,
                            onValueChange = { 
                                apiUrl = it
                                viewModel.prefs.aiApiUrl = it
                                testResult = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { 
                                Text(stringResource(Res.strings.ai_settings_api_url_hint)) 
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            trailingIcon = {
                                if (apiUrl.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        apiUrl = ""
                                        viewModel.prefs.aiApiUrl = ""
                                        testResult = null
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear"
                                        )
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show current status
                        val displayUrl = apiUrl.ifBlank { 
                            stringResource(Res.strings.ai_settings_api_url_default) 
                        }
                        Text(
                            text = "Current: $displayUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsGroup {
                    // Test and Reset buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isTesting = true
                                        testResult = null
                                        val result = try {
                                            val client = HttpClient()
                                            val url = apiUrl.ifBlank { 
                                                "http://10.0.2.2:3001" 
                                            }
                                            val response = client.get("$url/api/chat")
                                            val body = response.bodyAsText()
                                            client.close()
                                            
                                            if (response.status.value == 200 && body.contains("\"status\":\"ok\"")) {
                                                successMsg
                                            } else {
                                                "$failedMsg (HTTP ${response.status.value})"
                                            }
                                        } catch (e: Exception) {
                                            "$failedMsg: ${e.message}"
                                        }
                                        testResult = result
                                        isTesting = false
                                    }
                                },
                                enabled = !isTesting,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(Res.strings.ai_settings_test_running))
                                } else {
                                    Text(stringResource(Res.strings.ai_settings_test))
                                }
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    apiUrl = ""
                                    viewModel.prefs.aiApiUrl = ""
                                    testResult = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(Res.strings.ai_settings_reset))
                            }
                        }
                        
                        // Test result
                        testResult?.let { result ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.contains(successMsg)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Toolbar(
        scrollBehavior: TopAppBarScrollBehavior
    ) {
        LargeToolbar(
            title = stringResource(Res.strings.settings_ai),
            scrollBehavior = scrollBehavior
        )
    }
}
