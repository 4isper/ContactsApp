package com.m4isper.contactsapp.testing

import com.m4isper.contactsapp.FakeContactsChangeSource
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.usecase.contacts.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetAccountSyncInfosUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsPageUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetProfileContactIdUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.RefreshFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ToggleFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import com.m4isper.contactsapp.presentation.contacts.ContactsViewModel
import com.m4isper.contactsapp.presentation.contacts.state.ContactsFavoriteState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsFilterState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsGroupState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsListState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsSelectionState

/**
 * Граф зависимостей экрана списка для тестов: собирает части состояния и
 * use cases из общих фейков, чтобы тесту не приходилось строить его вручную.
 */
internal class ContactsTestGraph(
    val repository: FakeContactsRepository = FakeContactsRepository(),
    val groups: FakeGroupsRepository = FakeGroupsRepository(),
    val settings: FakeSettingsRepository = FakeSettingsRepository(),
    val changes: FakeContactsChangeSource = FakeContactsChangeSource(),
    val codec: FakeVCardCodec = FakeVCardCodec(),
) {
    /** Экран списка на общих фейках. */
    fun viewModel(): ContactsViewModel {
        val selection = ContactsSelectionState()
        val filters = ContactsFilterState(selection)
        return ContactsViewModel(
            list = ContactsListState(GetContactsPageUseCase(repository), changes),
            filters = filters,
            selection = selection,
            favorites =
                ContactsFavoriteState(
                    observeFavorites = ObserveFavoritesUseCase(repository),
                    toggleFavorite = ToggleFavoriteUseCase(repository),
                    removeFavorite = RemoveFavoriteUseCase(repository),
                    refreshFavorites = RefreshFavoritesUseCase(repository),
                ),
            groups =
                ContactsGroupState(
                    getGroups = GetGroupsUseCase(groups),
                    getMembership = GetGroupMembershipUseCase(groups),
                    getAccounts = GetContactAccountsUseCase(repository),
                    getAccountSyncInfos = GetAccountSyncInfosUseCase(repository),
                    getProfileContactId = GetProfileContactIdUseCase(repository),
                    createGroup = CreateGroupUseCase(groups),
                    addContactsToGroup = AddContactsToGroupUseCase(groups),
                    filters = filters,
                    selection = selection,
                ),
            deleteContactsUseCase = DeleteContactsUseCase(repository, repository),
            exportContactsUseCase =
                ExportContactsUseCase(
                    getContactDetails = GetContactDetailsUseCase(repository),
                    loadContactPhoto = LoadContactPhotoUseCase(repository),
                    getGroups = GetGroupsUseCase(groups),
                    getGroupMembership = GetGroupMembershipUseCase(groups),
                    codec = codec,
                ),
            getContactsUseCase = GetContactsUseCase(repository),
            observeSettingsUseCase = ObserveSettingsUseCase(settings),
        )
    }
}
