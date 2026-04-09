package dev.materii.gloom.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.domain.manager.enums.DownloadHandlerPreference
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.settings.component.SettingsGroup
import dev.materii.gloom.ui.screen.settings.component.SettingsHeader
import dev.materii.gloom.ui.screen.settings.component.SettingsItemChoice
import dev.materii.gloom.ui.screen.settings.viewmodel.DownloadsSettingsViewModel
import dev.materii.gloom.util.getString

class DownloadsSettingsScreen : Screen {

    @Composable
    override fun Content() = Screen()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen(
        viewModel: DownloadsSettingsViewModel = koinScreenModel()
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                SettingsHeader(stringResource(Res.strings.settings_downloads))
                SettingsGroup {
                    SettingsItemChoice(
                        label = stringResource(Res.strings.downloads_handler),
                        pref = viewModel.prefs.downloadHandler,
                        onPrefChange = { viewModel.prefs.downloadHandler = it },
                        title = stringResource(Res.strings.downloads_handler_description),
                        labelFactory = {
                            when (it) {
                                DownloadHandlerPreference.SYSTEM -> getString(Res.strings.downloads_handler_system)
                                DownloadHandlerPreference.EXTERNAL -> getString(Res.strings.downloads_handler_external)
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Toolbar(scrollBehavior: TopAppBarScrollBehavior) {
        LargeToolbar(
            title = stringResource(Res.strings.settings_downloads),
            scrollBehavior = scrollBehavior
        )
    }
}
