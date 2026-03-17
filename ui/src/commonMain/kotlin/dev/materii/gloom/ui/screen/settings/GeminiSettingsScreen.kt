package dev.materii.gloom.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.settings.viewmodel.GeminiSettingsViewModel

class GeminiSettingsScreen : Screen {

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun Content() {
        val viewModel: GeminiSettingsViewModel = koinScreenModel()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        Scaffold(
            topBar = { Toolbar(scrollBehavior) },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { pv ->
            Column(
                modifier = Modifier
                    .padding(pv)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            ) {
                var apiKey by remember { mutableStateOf(viewModel.apiKey) }
                var showKey by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(Res.strings.gemini_api_key)) },
                    placeholder = { Text(stringResource(Res.strings.gemini_api_key_placeholder)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null
                        )
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "Hide" else "Show")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(Res.strings.gemini_api_key_description),
                    modifier = Modifier.padding(top = 8.dp)
                )

                TextButton(
                    onClick = { viewModel.saveApiKey(apiKey) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(Res.strings.gemini_api_key_saved))
                }

                if (apiKey.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.clearApiKey()
                            apiKey = ""
                        }
                    ) {
                        Text(stringResource(Res.strings.gemini_api_key_cleared))
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun Toolbar(
        scrollBehavior: TopAppBarScrollBehavior
    ) {
        LargeToolbar(
            title = stringResource(Res.strings.settings_gemini),
            scrollBehavior = scrollBehavior
        )
    }

}
