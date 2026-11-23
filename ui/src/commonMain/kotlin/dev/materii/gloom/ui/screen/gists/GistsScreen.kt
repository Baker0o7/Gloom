package dev.materii.gloom.ui.screen.gists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.materii.gloom.api.dto.gist.Gist
import dev.materii.gloom.ui.component.LoadStateBox
import dev.materii.gloom.ui.component.ScreenScaffold
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.screen.gists.viewmodel.GistsViewModel
import dev.materii.gloom.util.TimeUtils
import kotlinx.datetime.Instant

class GistsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<GistsViewModel>()
        val snackbar = remember { SnackbarHostState() }
        var showCreate by remember { mutableStateOf(false) }

        LaunchedEffect(viewModel.message) {
            viewModel.message?.let {
                snackbar.showSnackbar(it)
                viewModel.message = null
            }
        }

        ScreenScaffold(
            title = "Gists",
            snackbar = snackbar,
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New gist")
                }
            }
        ) { pv ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading && viewModel.gists.isNotEmpty(),
                onRefresh = { viewModel.load() },
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                LoadStateBox(
                    isLoading = viewModel.isLoading,
                    hasError = viewModel.hasError,
                    isEmpty = viewModel.gists.isEmpty(),
                    emptyText = "You don't have any gists yet.\nTap + to create one.",
                    errorText = "Couldn't load your gists. Your token may be missing the gist permission.",
                    onRetry = { viewModel.load() }
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(viewModel.gists, key = { it.id }) { gist ->
                            GistItem(gist, onClick = { viewModel.open(gist) })
                            ThinDivider()
                        }
                    }
                }
            }
        }

        if (showCreate) {
            CreateGistDialog(
                isSaving = viewModel.isSaving,
                onDismiss = { showCreate = false },
                onCreate = { description, fileName, content, isPublic ->
                    viewModel.create(description, fileName, content, isPublic) { showCreate = false }
                }
            )
        }

        viewModel.detail?.let { detail ->
            GistDetailDialog(
                detail = detail,
                onDismiss = viewModel::closeDetail,
                onDelete = { viewModel.delete(detail.summary.id) }
            )
        }
    }
}

@Composable
private fun GistItem(gist: Gist, onClick: () -> Unit) {
    val firstFile = gist.files.values.firstOrNull()?.filename ?: "(empty)"
    val time = remember(gist.updatedAt) {
        gist.updatedAt?.let { runCatching { TimeUtils.getTimeSince(Instant.parse(it)) }.getOrNull() }
    }
    ListItem(
        headlineContent = {
            Text(
                gist.description?.takeIf { it.isNotBlank() } ?: firstFile,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val extra = gist.files.size - 1
            Text(
                buildString {
                    append(firstFile)
                    if (extra > 0) append(" +$extra more")
                    if (time != null) append(" · $time")
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (!gist.isPublic) Icon(Icons.Outlined.Lock, contentDescription = "Secret gist")
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun CreateGistDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (description: String, fileName: String, content: String, isPublic: Boolean) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("snippet.txt") }
    var content by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("New gist") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileName, onValueChange = { fileName = it },
                    label = { Text("File name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("Content") }, minLines = 5, maxLines = 10,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    Spacer(Modifier.width(12.dp))
                    Text(if (isPublic) "Public: anyone can see it" else "Secret: only people with the link")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(description.trim(), fileName.trim(), content, isPublic) },
                enabled = !isSaving && fileName.isNotBlank() && content.isNotBlank()
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } }
    )
}

@Composable
private fun GistDetailDialog(
    detail: GistsViewModel.Detail,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val files = (detail.full ?: detail.summary).files.values.toList()
    val allText = files.joinToString("\n\n") { it.content.orEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                detail.summary.description?.takeIf { it.isNotBlank() }
                    ?: files.firstOrNull()?.filename ?: "Gist",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            when {
                detail.failed -> Text("Couldn't load this gist.")
                detail.full == null -> CircularProgressIndicator()
                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    files.forEach { file ->
                        Text(file.filename, style = MaterialTheme.typography.labelLarge)
                        Text(
                            file.content ?: "(content not available)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            softWrap = false,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                        if (file.truncated) {
                            Text(
                                "This file is large, so only part of it is shown.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = { clipboard.setText(AnnotatedString(allText)) },
                    enabled = detail.full != null
                ) { Text("Copy") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this gist?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
