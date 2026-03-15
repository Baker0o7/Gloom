package dev.materii.gloom.di

import dev.materii.gloom.api.repository.ChatRepository
import dev.materii.gloom.api.repository.GithubAuthRepository
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.repository.GraphQLRepository
import dev.materii.gloom.domain.manager.PreferenceManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun repositoryModule() = module {

    singleOf(::GithubAuthRepository)
    singleOf(::GithubRepository)
    singleOf(::GraphQLRepository)
    // ChatRepository needs PreferenceManager for user API key
    single { ChatRepository(get(), get<PreferenceManager>()) }

}