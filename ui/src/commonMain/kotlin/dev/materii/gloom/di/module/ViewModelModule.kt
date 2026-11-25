package dev.materii.gloom.di.module

import dev.materii.gloom.domain.manager.AppUpdater
import dev.materii.gloom.ui.screen.actions.viewmodel.ActionsViewModel
import dev.materii.gloom.ui.screen.update.viewmodel.UpdateViewModel
import dev.materii.gloom.ui.screen.activity.viewmodel.CommitActivityViewModel
import dev.materii.gloom.ui.screen.dev.viewmodel.DevelopersViewModel
import dev.materii.gloom.ui.screen.discussions.viewmodel.DiscussionViewModel
import dev.materii.gloom.ui.screen.discussions.viewmodel.DiscussionsViewModel
import dev.materii.gloom.ui.screen.gists.viewmodel.GistsViewModel
import dev.materii.gloom.ui.screen.projects.viewmodel.ProjectBoardViewModel
import dev.materii.gloom.ui.screen.projects.viewmodel.ProjectsViewModel
import dev.materii.gloom.ui.screen.editor.viewmodel.CodeEditorViewModel
import dev.materii.gloom.ui.screen.notifications.viewmodel.NotificationsViewModel
import dev.materii.gloom.ui.screen.issue.viewmodel.CreateIssueViewModel
import dev.materii.gloom.ui.screen.search.viewmodel.SearchViewModel
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
import dev.materii.gloom.ui.screen.settings.viewmodel.DownloadsSettingsViewModel
import dev.materii.gloom.ui.screen.settings.viewmodel.SettingsViewModel
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
    factoryOf(::DownloadsSettingsViewModel)
    factoryOf(::AccountSettingsViewModel)
    factoryOf(::AppIconSettingsViewModel)
    factoryOf(::HomeViewModel)
    factoryOf(::ActionsViewModel)
    factory { UpdateViewModel(get(), getOrNull<AppUpdater>()) }
    factoryOf(::GistsViewModel)
    factoryOf(::DevelopersViewModel)
    factoryOf(::CommitActivityViewModel)
    factoryOf(::DiscussionsViewModel)
    factoryOf(::DiscussionViewModel)
    factoryOf(::ProjectsViewModel)
    factoryOf(::ProjectBoardViewModel)
    factoryOf(::CodeEditorViewModel)
    factoryOf(::ExploreViewModel)
    // Singleton so badge in navbar and inbox screen share the same unread count
    singleOf(::NotificationsViewModel)

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
    factoryOf(::SearchViewModel)
    factoryOf(::CreateIssueViewModel)

}
