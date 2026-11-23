package dev.materii.gloom.ui.screen.gists.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.gist.Gist
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import kotlinx.coroutines.launch

class GistsViewModel(
    private val githubRepo: GithubRepository,
) : ScreenModel {

    /** [full] carries file contents once loaded. */
    data class Detail(val summary: Gist, val full: Gist? = null, val failed: Boolean = false)

    val gists = mutableStateListOf<Gist>()

    var isLoading by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var detail by mutableStateOf<Detail?>(null)
        private set

    var message by mutableStateOf<String?>(null)

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            isLoading = true
            hasError = false
            when (val response = githubRepo.getGists()) {
                is ApiResponse.Success -> {
                    gists.clear()
                    gists.addAll(response.data)
                }
                is ApiResponse.Empty -> gists.clear()
                is ApiResponse.Error -> hasError = true
                is ApiResponse.Failure -> hasError = true
            }
            isLoading = false
        }
    }

    fun open(gist: Gist) {
        detail = Detail(gist)
        screenModelScope.launch {
            val response = githubRepo.getGist(gist.id)
            detail = if (response is ApiResponse.Success) Detail(gist, full = response.data)
            else Detail(gist, failed = true)
        }
    }

    fun closeDetail() {
        detail = null
    }

    fun delete(id: String) {
        screenModelScope.launch {
            when (githubRepo.deleteGist(id)) {
                is ApiResponse.Success, is ApiResponse.Empty -> {
                    gists.removeAll { it.id == id }
                    detail = null
                    message = "Gist deleted"
                }
                is ApiResponse.Error -> message = "Couldn't delete the gist. Your token may be missing the gist permission."
                is ApiResponse.Failure -> message = "Network error. Please try again."
            }
        }
    }

    fun create(description: String, fileName: String, content: String, isPublic: Boolean, onDone: () -> Unit) {
        if (isSaving) return
        screenModelScope.launch {
            isSaving = true
            try {
                when (val response = githubRepo.createGist(description, isPublic, fileName, content)) {
                    is ApiResponse.Success -> {
                        gists.add(0, response.data)
                        message = "Gist created"
                        onDone()
                    }
                    is ApiResponse.Empty -> message = "Gist created"
                    is ApiResponse.Error -> message = "Couldn't create the gist. Your token may be missing the gist permission."
                    is ApiResponse.Failure -> message = "Network error. Please try again."
                }
            } finally {
                isSaving = false
            }
        }
    }
}
