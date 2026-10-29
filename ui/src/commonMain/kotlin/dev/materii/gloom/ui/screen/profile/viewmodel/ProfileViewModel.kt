package dev.materii.gloom.ui.screen.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.model.ModelUser
import dev.materii.gloom.api.repository.GraphQLRepository
import dev.materii.gloom.api.util.fold
import dev.materii.gloom.api.util.ifSuccessful
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val gqlRepo: GraphQLRepository,
    private val username: String
): ScreenModel {

    var user: ModelUser? by mutableStateOf(null)
    var hasError by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)

    private var isTogglingFollow = false

    init {
        loadData()
    }

    fun loadData() {
        if (isLoading) return
        isLoading = true
        hasError = false
        screenModelScope.launch(Dispatchers.IO) {
            try {
                val response = if (username.isNotEmpty()) gqlRepo.getProfile(username)
                               else gqlRepo.getCurrentProfile()
                response.fold(
                    onSuccess = { profile ->
                        if (profile.username == null) {
                            hasError = true
                        } else {
                            user = profile
                        }
                    },
                    onError = { hasError = true }
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFollowing() {
        val currentUser = user ?: return
        val userId = currentUser.id ?: return
        val isFollowing = currentUser.isFollowing
        if (isTogglingFollow) return
        isTogglingFollow = true

        screenModelScope.launch {
            try {
                if (isFollowing == false)
                    gqlRepo.followUser(userId).ifSuccessful {
                        user = currentUser.copy(isFollowing = it.first)
                    }
                else
                    gqlRepo.unfollowUser(userId).ifSuccessful {
                        user = currentUser.copy(isFollowing = it.first)
                    }
            } finally {
                isTogglingFollow = false
            }
        }
    }

}