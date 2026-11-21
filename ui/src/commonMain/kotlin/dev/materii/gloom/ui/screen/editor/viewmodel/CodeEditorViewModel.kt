package dev.materii.gloom.ui.screen.editor.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class CodeEditorViewModel(
    private val githubRepo: GithubRepository,
    private val input: Input,
) : ScreenModel {

    data class Input(val owner: String, val name: String, val branch: String, val path: String)

    var original by mutableStateOf("")
        private set

    var text by mutableStateOf(TextFieldValue(""))

    var isLoading by mutableStateOf(true)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)

    /** The branch commits go to; changes to the new branch after "commit to a new branch". */
    var currentBranch by mutableStateOf(input.branch)
        private set

    var showFind by mutableStateOf(false)
    var findQuery by mutableStateOf("")

    private var sha = ""

    val isDirty: Boolean get() = text.text != original

    val matchCount: Int
        get() {
            val q = findQuery
            if (q.isEmpty()) return 0
            val t = text.text
            var count = 0
            var i = t.indexOf(q, 0, ignoreCase = true)
            while (i >= 0) {
                count++
                i = t.indexOf(q, i + q.length, ignoreCase = true)
            }
            return count
        }

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            isLoading = true
            loadError = null
            when (val response = githubRepo.getFileContent(input.owner, input.name, input.path, currentBranch)) {
                is ApiResponse.Success -> {
                    val file = response.data
                    val decoded = if (file.encoding == "base64" && file.content.isNotEmpty()) {
                        runCatching { Base64.Mime.decode(file.content).decodeToString() }.getOrNull()
                    } else null

                    when {
                        file.size > MAX_EDITABLE_BYTES ->
                            loadError = "This file is too large to edit here."
                        decoded == null ->
                            loadError = "This file can't be opened for editing."
                        decoded.contains('\u0000') ->
                            loadError = "Binary files can't be edited."
                        else -> {
                            sha = file.sha
                            original = decoded
                            text = TextFieldValue(decoded)
                        }
                    }
                }
                is ApiResponse.Empty -> loadError = "This file is empty or unavailable."
                is ApiResponse.Error -> loadError = "Couldn't load the file."
                is ApiResponse.Failure -> loadError = "Network error. Please try again."
            }
            isLoading = false
        }
    }

    // ── Find ──────────────────────────────────────────────────────────────────

    fun findNext(forward: Boolean) {
        val q = findQuery
        if (q.isEmpty()) return
        val t = text.text
        val index = if (forward) {
            t.indexOf(q, text.selection.max, ignoreCase = true).let {
                if (it < 0) t.indexOf(q, 0, ignoreCase = true) else it
            }
        } else {
            val start = text.selection.min - 1
            (if (start >= 0) t.lastIndexOf(q, start, ignoreCase = true) else -1).let {
                if (it < 0) t.lastIndexOf(q, t.length, ignoreCase = true) else it
            }
        }
        if (index >= 0) text = text.copy(selection = TextRange(index, index + q.length))
    }

    /** Zero-based line of the cursor / current match. */
    fun currentLine(): Int = text.text.take(text.selection.min).count { it == '\n' }

    // ── Commit ────────────────────────────────────────────────────────────────

    /**
     * Commits the edited file. With [newBranch] set, that branch is first created from the
     * current one and the commit goes there.
     */
    fun commit(message: String, newBranch: String?) {
        if (isSaving) return
        screenModelScope.launch {
            isSaving = true
            try {
                var targetBranch = currentBranch

                if (newBranch != null) {
                    val ref = githubRepo.getBranchRef(input.owner, input.name, currentBranch)
                    val baseSha = if (ref is ApiResponse.Success) ref.data.target.sha else null
                    if (baseSha == null) {
                        statusMessage = "Couldn't read the current branch."
                        return@launch
                    }
                    val created = githubRepo.createBranch(input.owner, input.name, newBranch, baseSha)
                    if (created is ApiResponse.Error || created is ApiResponse.Failure) {
                        statusMessage = "Couldn't create branch '$newBranch'. It may already exist."
                        return@launch
                    }
                    targetBranch = newBranch
                }

                val content = Base64.encode(text.text.encodeToByteArray())
                when (val result = githubRepo.updateFile(
                    input.owner, input.name, input.path, message, content, sha, targetBranch
                )) {
                    is ApiResponse.Success, is ApiResponse.Empty -> {
                        currentBranch = targetBranch
                        original = text.text
                        statusMessage = "Committed to $targetBranch"
                        // The blob sha changed; fetch it so a follow-up commit doesn't conflict.
                        val fresh = githubRepo.getFileContent(input.owner, input.name, input.path, targetBranch)
                        if (fresh is ApiResponse.Success) sha = fresh.data.sha
                    }
                    is ApiResponse.Error -> {
                        val conflict = result.error.message?.contains("409") == true
                        statusMessage = if (conflict)
                            "The file changed on GitHub. Reload it and apply your edit again."
                        else
                            "Commit failed. Your token may lack write access to this repository."
                    }
                    is ApiResponse.Failure -> statusMessage = "Network error. Please try again."
                }
            } finally {
                isSaving = false
            }
        }
    }

    private companion object {
        const val MAX_EDITABLE_BYTES = 300_000L
    }
}
