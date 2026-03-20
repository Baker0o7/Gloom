package dev.materii.gloom.ui.screen.issue

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Send
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.issue.viewmodel.CreateIssueViewModel
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class CreateIssueScreen(
    private val owner: String,
    private val repo: String,
) : Screen {

    override val key: ScreenKey get() = "$owner/$repo/create-issue"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel   = koinScreenModel<CreateIssueViewModel> { parametersOf(owner, repo) }
        val nav         = LocalNavigator.currentOrThrow
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val snackbar    = remember { SnackbarHostState() }
        val scope       = rememberCoroutineScope()

        var title by remember { mutableStateOf("") }
        var body  by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                LargeToolbar(
                    title          = "New Issue",
                    scrollBehavior = scrollBehavior,
                    actions = {
                        // AI draft button
                        IconButton(
                            onClick  = { viewModel.aiDraftBody(title) { body = it } },
                            enabled  = title.isNotBlank() && !viewModel.aiLoading,
                        ) {
                            if (viewModel.aiLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.AutoAwesome, "AI draft body")
                            }
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "$owner/$repo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Title") },
                    singleLine    = true,
                )

                OutlinedTextField(
                    value         = body,
                    onValueChange = { body = it },
                    modifier      = Modifier.fillMaxWidth().height(220.dp),
                    label         = { Text("Description (optional)") },
                    placeholder   = { Text("Describe the issue…\n\nTip: tap ✨ to let AI draft this for you.") },
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val ok = viewModel.submit(title, body)
                                if (ok) nav.pop()
                                else snackbar.showSnackbar("Failed to create issue. Try again.")
                            }
                        },
                        enabled  = title.isNotBlank() && !viewModel.isSubmitting,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (viewModel.isSubmitting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Outlined.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Submit Issue")
                    }
                }
            }
        }
    }
}
