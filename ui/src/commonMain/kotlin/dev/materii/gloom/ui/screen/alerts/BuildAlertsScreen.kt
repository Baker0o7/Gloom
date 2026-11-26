package dev.materii.gloom.ui.screen.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.materii.gloom.domain.manager.BuildAlerts
import dev.materii.gloom.ui.component.ScreenScaffold
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.screen.repo.RepoScreen
import org.koin.compose.getKoin

/** Lists every repository with failed-build alerts on, so they can be reviewed or turned off in one place. */
class BuildAlertsScreen : Screen {

    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        val koin = getKoin()
        val alerts = remember(koin) { koin.getOrNull<BuildAlerts>() }
        val repos = remember { mutableStateListOf<String>().apply { alerts?.let { addAll(it.watchedRepos()) } } }

        ScreenScaffold(title = "Build alerts", subtitle = "Failed workflow runs, checked about every 15 minutes") { pv ->
            if (repos.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(pv), Alignment.Center) {
                    Text(
                        "No repositories yet. Open a repository's Actions screen and tap the bell to get alerted when a build fails.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(pv)) {
                    items(repos.toList(), key = { it }) { fullName ->
                        val owner = fullName.substringBefore('/')
                        val name = fullName.substringAfter('/')
                        ListItem(
                            headlineContent = { Text(fullName) },
                            trailingContent = {
                                IconButton(onClick = {
                                    alerts?.setWatching(owner, name, false)
                                    repos.remove(fullName)
                                }) {
                                    Icon(Icons.Outlined.NotificationsOff, contentDescription = "Turn off alerts for $fullName")
                                }
                            },
                            modifier = Modifier.clickable { nav.push(RepoScreen(owner, name)) },
                        )
                        ThinDivider()
                    }
                }
            }
        }
    }
}
