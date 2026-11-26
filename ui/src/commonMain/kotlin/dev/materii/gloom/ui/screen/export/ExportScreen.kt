package dev.materii.gloom.ui.screen.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.materii.gloom.domain.manager.ShareManager
import dev.materii.gloom.ui.component.ScreenScaffold
import dev.materii.gloom.ui.screen.export.viewmodel.ExportViewModel
import org.koin.compose.koinInject

class ExportScreen : Screen {

    @Composable
    override fun Content() {
        val vm = koinScreenModel<ExportViewModel>()
        val share: ShareManager = koinInject()
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(vm.payload) {
            vm.payload?.let {
                share.shareText(it)
                vm.consume()
            }
        }
        LaunchedEffect(vm.message) {
            vm.message?.let {
                snackbar.showSnackbar(it)
                vm.message = null
            }
        }

        ScreenScaffold(title = "Export your data", subtitle = "Save a copy as JSON", snackbar = snackbar) { pv ->
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(pv)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Exports open the share sheet, so you can save them to Drive, Files, notes or send them to yourself. " +
                        "Nothing is uploaded anywhere else, and your access token is never included.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                vm.kinds.forEach { kind ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(kind.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                kind.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(onClick = { vm.export(kind) }, enabled = vm.busy == null) {
                                if (vm.busy == kind) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Export")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
