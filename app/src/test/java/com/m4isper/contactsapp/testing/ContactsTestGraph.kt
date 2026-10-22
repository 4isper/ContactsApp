package com.m4isper.contactsapp.testing

import com.m4isper.contactsapp.FakeContactsChangeSource
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsPageUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.GetProfileContactIdUseCase
import com.m4isper.contactsapp.domain.usecase.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.RefreshFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.contacts.ContactsFavoriteState
import com.m4isper.contactsapp.presentation.contacts.ContactsFilterState
import com.m4isper.contactsapp.presentation.contacts.ContactsGroupState
import com.m4isper.contactsapp.presentation.contacts.ContactsListState
import com.m4isper.contactsapp.presentation.contacts.ContactsSelectionState
import com.m4isper.contactsapp.presentation.contacts.ContactsViewModel

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
