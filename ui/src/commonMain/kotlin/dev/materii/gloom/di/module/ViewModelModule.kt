package dev.materii.gloom.di.module

import dev.materii.gloom.ui.screen.auth.viewmodel.LandingViewModel
import dev.materii.gloom.ui.screen.explore.viewmodel.ExploreViewModel
import dev.materii.gloom.ui.screen.explorer.viewmodel.DirectoryListingViewModel
import dev.materii.gloom.ui.screen.explorer.viewmodel.FileViewerViewModel
import dev.materii.gloom.ui.screen.home.viewmodel.HomeViewModel
import dev.materii.gloom.ui.screen.list.viewmodel.*
import dev.materii.gloom.ui.screen.profile.viewmodel.FollowersViewModel
import dev.materii.gloom.ui.screen.profile.viewmodel.FollowingViewModel
import dev.materii.gloom.ui.screen.profile.viewmodel.ProfileViewModel
import dev.materii.gloom.ui.screen.repo.viewmodel.*
import dev.materii.gloom.ui.screen.settings.viewmodel.AccountSettingsViewModel
import dev.materii.gloom.ui.screen.settings.viewmodel.AppIconSettingsViewModel
import dev.materii.gloom.ui.screen.settings.viewmodel.AppearanceSettingsViewModel
import dev.materii.gloom.ui.screen.settings.viewmodel.SettingsViewModel
import dev.materii.gloom.ui.screen.settings.viewmodel.AiSettingsViewModel
import dev.materii.gloom.ui.screen.chat.viewmodel.ChatViewModel
import dev.materii.gloom.ui.screen.notifications.viewmodel.NotificationsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun viewModelModule() = module {

    factoryOf(::LandingViewModel)
    factoryOf(::ProfileViewModel)
    factoryOf(::RepositoryListViewModel)
    factoryOf(::StarredReposListViewModel)
    factoryOf(::ForksViewModel)
    factoryOf(::OrgListViewModel)
    factoryOf(::FollowersViewModel)
    factoryOf(::FollowingViewModel)
    factoryOf(::SponsoringViewModel)
    factoryOf(::SettingsViewModel)
    factoryOf(::AppearanceSettingsViewModel)
    factoryOf(::AccountSettingsViewModel)
    factoryOf(::AppIconSettingsViewModel)
    factoryOf(::AiSettingsViewModel)
    factoryOf(::HomeViewModel)
    factoryOf(::ExploreViewModel)
    factoryOf(::NotificationsViewModel)
    // ChatViewModel is a singleton so conversation persists across FAB open/close
    singleOf(::ChatViewModel)

    factoryOf(::RepoViewModel)
    factoryOf(::RepoDetailsViewModel)
    factoryOf(::RepoCodeViewModel)
    factoryOf(::DirectoryListingViewModel)
    factoryOf(::FileViewerViewModel)
    factoryOf(::RepoIssuesViewModel)
    factoryOf(::RepoPullRequestsViewModel)
    factoryOf(::RepoReleasesViewModel)
    factoryOf(::RepoCommitsViewModel)
    factoryOf(::RepoContributorsViewModel)
    factoryOf(::LicenseViewModel)

}