package dev.materii.gloom.ui.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.materii.gloom.ui.component.ErrorMessage
import dev.materii.gloom.ui.screen.editor.viewmodel.CodeEditorViewModel
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class CodeEditorScreen(
    private val owner: String,
    private val name: String,
    private val branch: String,
    private val path: String,
) : Screen {

    override val key: ScreenKey get() = "$owner/$name/edit/$branch/$path"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CodeEditorViewModel> {
            parametersOf(CodeEditorViewModel.Input(owner, name, branch, path))
        }
        val nav = LocalNavigator.currentOrThrow
        val snackbar = remember { SnackbarHostState() }
        var showCommit by remember { mutableStateOf(false) }
        var showDiscard by remember { mutableStateOf(false) }
        val fileName = path.substringAfterLast('/')

        LaunchedEffect(viewModel.statusMessage) {
            viewModel.statusMessage?.let {
                snackbar.showSnackbar(it)
                viewModel.statusMessage = null
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                fileName + if (viewModel.isDirty) " •" else "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                viewModel.currentBranch,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (viewModel.isDirty) showDiscard = true else nav.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.showFind = !viewModel.showFind }) {
                            Icon(Icons.Filled.Search, contentDescription = "Find in file")
                        }
                        IconButton(
                            onClick = { showCommit = true },
                            enabled = viewModel.isDirty && !viewModel.isSaving
                        ) {
                            if (viewModel.isSaving) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = "Commit changes")
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pv ->
            val error = viewModel.loadError
            Box(
                Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                when {
                    viewModel.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null -> ErrorMessage(
                        message = error,
                        modifier = Modifier.align(Alignment.Center),
                        onRetryClick = { viewModel.load() }
                    )
                    else -> EditorBody(viewModel)
                }
            }
        }

        if (showCommit) {
            val base = fileName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._-]"), "-")
            CommitDialog(
                defaultMessage = "Update $fileName",
                currentBranch = viewModel.currentBranch,
                defaultNewBranch = "edit/$base",
                onDismiss = { showCommit = false },
                onConfirm = { message, newBranch ->
                    showCommit = false
                    viewModel.commit(message, newBranch)
                }
            )
        }

        if (showDiscard) {
            AlertDialog(
                onDismissRequest = { showDiscard = false },
                title = { Text("Discard changes?") },
                text = { Text("You have uncommitted edits. Leaving now will lose them.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDiscard = false
                        nav.pop()
                    }) { Text("Discard") }
                },
                dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } }
            )
        }
    }
}

@Composable
private fun EditorBody(viewModel: CodeEditorViewModel) {
    var tab by remember { mutableStateOf(0) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val scope = rememberCoroutineScope()
    val lineHeight = 18.sp
    val lineHeightPx = with(LocalDensity.current) { lineHeight.toPx() }
    val highlight = MaterialTheme.colorScheme.tertiaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxSize()) {
        if (viewModel.showFind) {
            FindBar(viewModel) { forward ->
                viewModel.findNext(forward)
                scope.launch {
                    val target = ((viewModel.currentLine() - 3).coerceAtLeast(0) * lineHeightPx).toInt()
                    vertical.animateScrollTo(target)
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Edit") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Changes") })
        }

        if (tab == 0) {
            val style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = lineHeight,
                color = onSurface
            )
            val lineCount = remember(viewModel.text.text) { viewModel.text.text.count { it == '\n' } + 1 }
            val gutter = remember(lineCount) { (1..lineCount).joinToString("\n") }
            val query = if (viewModel.showFind) viewModel.findQuery else ""
            val transformation = remember(query, highlight) { SearchHighlightTransformation(query, highlight) }

            Row(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(vertical)
            ) {
                Text(
                    gutter,
                    style = style.copy(color = onSurface.copy(alpha = 0.4f), textAlign = TextAlign.End),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .widthIn(min = 28.dp)
                )
                Box(
                    Modifier
                        .weight(1f)
                        .horizontalScroll(horizontal)
                ) {
                    BasicTextField(
                        value = viewModel.text,
                        onValueChange = { viewModel.text = it },
                        textStyle = style,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = transformation,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 300.dp, minHeight = 300.dp)
                            .padding(end = 16.dp)
                    )
                }
            }
        } else {
            DiffView(viewModel)
        }
    }
}

@Composable
private fun FindBar(viewModel: CodeEditorViewModel, onNavigate: (forward: Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = viewModel.findQuery,
            onValueChange = { viewModel.findQuery = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Find in file") },
            trailingIcon = {
                if (viewModel.findQuery.isNotEmpty()) {
                    Text(
                        "${viewModel.matchCount}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        )
        IconButton(onClick = { onNavigate(false) }) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Previous match")
        }
        IconButton(onClick = { onNavigate(true) }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
        }
    }
}

@Composable
private fun DiffView(viewModel: CodeEditorViewModel) {
    val diff = remember(viewModel.text.text, viewModel.original) {
        diffLines(viewModel.original, viewModel.text.text)
    }

    if (diff.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No changes",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(diff.size) { index ->
            val line = diff[index]
            val (background, prefix) = when (line.type) {
                DiffType.ADD -> Color(0x3328A745) to "+ "
                DiffType.REMOVE -> Color(0x33F85149) to "- "
                DiffType.SAME -> Color.Transparent to "  "
            }
            Text(
                prefix + line.text,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .padding(horizontal = 12.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun CommitDialog(
    defaultMessage: String,
    currentBranch: String,
    defaultNewBranch: String,
    onDismiss: () -> Unit,
    onConfirm: (message: String, newBranch: String?) -> Unit,
) {
    var message by remember { mutableStateOf(defaultMessage) }
    var useNewBranch by remember { mutableStateOf(false) }
    var branchName by remember { mutableStateOf(defaultNewBranch) }
    val branchValid = Regex("^[A-Za-z0-9._/-]+\$").matches(branchName) &&
        !branchName.startsWith("/") && !branchName.endsWith("/")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit changes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Commit message") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = useNewBranch, onCheckedChange = { useNewBranch = it })
                    Spacer(Modifier.width(12.dp))
                    Text("Commit to a new branch", style = MaterialTheme.typography.bodyMedium)
                }
                if (useNewBranch) {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("New branch name") },
                        isError = !branchValid,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Commits directly to $currentBranch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(message.trim(), if (useNewBranch) branchName.trim() else null) },
                enabled = message.isNotBlank() && (!useNewBranch || branchValid)
            ) { Text("Commit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Highlights every occurrence of [query] so matches are visible without text selection. */
private class SearchHighlightTransformation(
    private val query: String,
    private val color: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (query.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val highlighted = buildAnnotatedString {
            append(text)
            var i = text.text.indexOf(query, 0, ignoreCase = true)
            while (i >= 0) {
                addStyle(SpanStyle(background = color), i, i + query.length)
                i = text.text.indexOf(query, i + query.length, ignoreCase = true)
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
