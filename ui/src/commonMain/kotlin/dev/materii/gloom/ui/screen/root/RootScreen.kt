package dev.materii.gloom.ui.screen.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.benasher44.uuid.uuid4
import dev.materii.gloom.domain.manager.AuthManager
import dev.materii.gloom.domain.manager.PreferenceManager
import dev.materii.gloom.ui.component.Avatar
import dev.materii.gloom.ui.component.navbar.LongClickableNavBarItem
import dev.materii.gloom.ui.screen.notifications.viewmodel.NotificationsViewModel
import dev.materii.gloom.ui.screen.settings.component.account.AccountSwitcherSheet
import dev.materii.gloom.ui.util.DimenUtil
import dev.materii.gloom.ui.util.RootTab
import org.koin.compose.koinInject

class RootScreen: Screen {

    override val key = "${this::class.qualifiedName}-${uuid4()}"

    @Composable
    override fun Content() = Screen()

    @Composable
    private fun Screen() {
        var accountSwitcherVisible by remember {
            mutableStateOf(false)
        }
        
        val prefs: PreferenceManager = koinInject()
        val aiEnabled = prefs.aiEnabled

        if (accountSwitcherVisible) {
            AccountSwitcherSheet(
                onDismiss = { accountSwitcherVisible = false }
            )
        }

        TabNavigator(tab = RootTab.HOME.tab) { nav ->
            // Hide FAB when on AI screen or AI is disabled
            val showFab = aiEnabled && nav.current != RootTab.AI.tab
            
            Scaffold(
                bottomBar = {
                    TabBar(
                        onProfileLongClick = { accountSwitcherVisible = true }
                    )
                },
                floatingActionButton = {
                    // AI Floating Action Button - hidden when on AI screen or AI is disabled
                    if (showFab) {
                        FloatingActionButton(
                            onClick = { nav.current = RootTab.AI.tab },
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = "AI Assistant"
                            )
                        }
                    }
                }
            ) {
                Box(Modifier.padding(bottom = it.calculateBottomPadding() - DimenUtil.navBarPadding)) {
                    nav.current.Content()
                }
            }
        }
    }

    @Composable
    private fun TabBar(
        onProfileLongClick: () -> Unit
    ) {
        val authManager: AuthManager = koinInject()
        val notifVM: NotificationsViewModel = koinInject()
        val navigator = LocalTabNavigator.current

        NavigationBar {
            RootTab.entries.filter { it != RootTab.AI }.forEach { rootTab ->
                val badge = if (rootTab == RootTab.NOTIFICATIONS && notifVM.unreadCount > 0)
                    notifVM.unreadCount else null

                LongClickableNavBarItem(
                    selected    = navigator.current == rootTab.tab,
                    onClick     = { navigator.current = rootTab.tab },
                    onLongClick = { if (rootTab == RootTab.PROFILE) onProfileLongClick() },
                    icon = {
                        BadgedBox(badge = {
                            if (badge != null) Badge { Text(badge.coerceAtMost(99).toString()) }
                        }) {
                            if (authManager.accounts.size > 1 && rootTab == RootTab.PROFILE) {
                                val avatarUrl = authManager.currentAccount?.avatarUrl
                                if (avatarUrl != null) {
                                    Avatar(
                                        url = avatarUrl,
                                        contentDescription = rootTab.tab.options.title,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .alpha(if (navigator.current == rootTab.tab) 1f else 0.75f)
                                    )
                                } else {
                                    Icon(rootTab.tab.options.icon!!, rootTab.tab.options.title)
                                }
                            } else {
                                Icon(rootTab.tab.options.icon!!, rootTab.tab.options.title)
                            }
                        }
                    },
                    label = { Text(text = rootTab.tab.options.title) },
                )
            }
        }
    }

}
