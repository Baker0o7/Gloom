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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
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

        var email          by rememberSaveable { mutableStateOf(viewModel.savedEmail) }
        var password       by rememberSaveable { mutableStateOf("") }
        var passVisible    by rememberSaveable { mutableStateOf(false) }
        var isLoading      by remember { mutableStateOf(false) }
        var aiEnabled      by rememberSaveable { mutableStateOf(viewModel.aiEnabled) }

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
                ZAiInfoCard()

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

                // ── Signed-in state ───────────────────────────────────────────
                if (viewModel.isSignedIn) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Signed in",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    viewModel.savedEmail.ifBlank { "Z.AI account" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            OutlinedButton(onClick = {
                                viewModel.signOut()
                                email    = ""
                                password = ""
                                scope.launch { snackbar.showSnackbar("Signed out") }
                            }) {
                                Icon(Icons.Outlined.Logout, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sign out")
                            }
                        }
                    }
                }

                // ── Login form ────────────────────────────────────────────────
                Text(
                    if (viewModel.isSignedIn) "Sign in with a different account" else "Sign in to Z.AI",
                    style = MaterialTheme.typography.titleSmall,
                )

                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Email") },
                    placeholder   = { Text("you@example.com") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = { Icon(Icons.Outlined.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Password") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = { Icon(Icons.Outlined.Lock, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                if (passVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passVisible) "Hide" else "Show",
                            )
                        }
                    },
                )

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = viewModel.signIn(email, password)
                            isLoading = false
                            result.fold(
                                onSuccess = {
                                    password = ""
                                    snackbar.showSnackbar("Signed in successfully!")
                                },
                                onFailure = { snackbar.showSnackbar("Sign in failed: ${it.message}") }
                            )
                        }
                    },
                    enabled  = email.isNotBlank() && password.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Sign in to Z.AI")
                }

                // Help card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, null,
                            Modifier.size(16.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Don't have an account? Sign up at z.ai — free tier includes access to GLM-4 Flash.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ZAiInfoCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
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
                Icon(Icons.Outlined.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Z.AI — GLM Smart Assistant",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                "Sign in with your z.ai account to enable the AI assistant. Free tier includes GLM-4 Flash.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
