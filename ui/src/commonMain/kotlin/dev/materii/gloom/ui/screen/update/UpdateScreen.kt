package dev.materii.gloom.ui.screen.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.materii.gloom.ui.component.ScreenScaffold
import dev.materii.gloom.ui.screen.update.viewmodel.UpdateViewModel
import dev.materii.gloom.ui.screen.update.viewmodel.UpdateViewModel.Phase
import dev.materii.gloom.util.VersionName

class UpdateScreen : Screen {

    @Composable
    override fun Content() {
        val vm = koinScreenModel<UpdateViewModel>()

        LaunchedEffect(Unit) { if (vm.phase == Phase.IDLE) vm.check() }

        ScreenScaffold(title = "App updates", subtitle = "Installed: v$VersionName") { pv ->
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(pv)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val latest = vm.latest
                when (vm.phase) {
                    Phase.IDLE, Phase.CHECKING -> {
                        Text("Checking for updates…", style = MaterialTheme.typography.bodyLarge)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    Phase.UP_TO_DATE -> {
                        Text("You're up to date", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Latest release is ${latest?.tagName.orEmpty()}.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = vm::check) { Text("Check again") }
                    }
                    Phase.AVAILABLE, Phase.DOWNLOADING, Phase.READY -> {
                        Text("${latest?.tagName.orEmpty()} is available", style = MaterialTheme.typography.titleMedium)
                        val notes = latest?.body?.trim().orEmpty()
                        if (notes.isNotEmpty()) {
                            Text(
                                notes.take(1500) + if (notes.length > 1500) "…" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when (vm.phase) {
                            Phase.DOWNLOADING -> {
                                LinearProgressIndicator(progress = { vm.progress }, modifier = Modifier.fillMaxWidth())
                                Text("Downloading… ${(vm.progress * 100).toInt()}%")
                            }
                            Phase.READY -> {
                                if (vm.needsPermission) {
                                    Text(
                                        "Allow Gloom to install apps in the settings page that just opened, then come back and tap Install.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Button(onClick = vm::install, modifier = Modifier.fillMaxWidth()) { Text("Install") }
                            }
                            else -> Button(onClick = vm::startUpdate, modifier = Modifier.fillMaxWidth()) {
                                Text("Download and install")
                            }
                        }
                    }
                    Phase.ERROR -> {
                        Text(
                            vm.error ?: "Something went wrong",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = vm::check) { Text("Try again") }
                    }
                }
            }
        }
    }
}
