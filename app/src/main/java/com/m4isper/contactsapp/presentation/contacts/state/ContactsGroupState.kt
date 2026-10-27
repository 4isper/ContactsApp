package com.m4isper.contactsapp.presentation.contacts.state

import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.usecase.contacts.GetAccountSyncInfosUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetProfileContactIdUseCase
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.presentation.common.safeCall
import com.m4isper.contactsapp.presentation.contacts.ContactsEvent
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

private const val GROUPS_TAG = "ContactsGroups"

/**
 * Группы, членство, аккаунты и профиль: часть состояния списка, которая
 * перечитывается вместе с контактами.
 */
@Inject
internal class ContactsGroupState(
    private val getGroups: GetGroupsUseCase,
    private val getMembership: GetGroupMembershipUseCase,
    private val getAccounts: GetContactAccountsUseCase,
    private val getAccountSyncInfos: GetAccountSyncInfosUseCase,
    private val getProfileContactId: GetProfileContactIdUseCase,
    private val createGroup: CreateGroupUseCase,
    private val addContactsToGroup: AddContactsToGroupUseCase,
    private val filters: ContactsFilterState,
    private val selection: ContactsSelectionState,
) {
    private val _events = Channel<ContactsEvent>(Channel.BUFFERED)

    /** События сценариев групп: экран показывает их пользователю. */
    val events: Flow<ContactsEvent> = _events.receiveAsFlow()

    private val _groups = MutableStateFlow<List<ContactGroup>>(emptyList())

    /** Группы контактов. */
    val groups: StateFlow<List<ContactGroup>> = _groups.asStateFlow()
    private val _membership = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())

    /** Членство контактов: contactId → id групп. */
    val membership: StateFlow<Map<Long, Set<Long>>> = _membership.asStateFlow()
    private val _accounts = MutableStateFlow<List<ContactAccount>>(emptyList())

    /** Аккаунты для фильтра и создания. */
    val accounts: StateFlow<List<ContactAccount>> = _accounts.asStateFlow()
    private val _accountSyncInfos = MutableStateFlow<List<AccountSyncInfo>>(emptyList())

    /** Статус синхронизации аккаунтов для индикатора в UI. */
    val accountSyncInfos: StateFlow<List<AccountSyncInfo>> = _accountSyncInfos.asStateFlow()
    private val _profileContactId = MutableStateFlow<Long?>(null)

    /** id контакта «Мой профиль»; null, если профиля нет. */
    val profileContactId: StateFlow<Long?> = _profileContactId.asStateFlow()

    /** Перечитывает группы, членство, аккаунты и профиль. */
    suspend fun refresh() {
        refreshGroups()
        refreshAccounts()
        refreshProfile()
    }

    /** Сбрасывает группы, аккаунты и профиль: экран закрыт, состояние не нужно. */
    fun clear() {
        _groups.value = emptyList()
        _membership.value = emptyMap()
        _accounts.value = emptyList()
        _accountSyncInfos.value = emptyList()
        _profileContactId.value = null
    }

    /** Создаёт группу из фильтра: список групп и членство перечитываются. */
    suspend fun onCreateGroup(title: String) {
        if (title.isBlank()) return
        safeCall(GROUPS_TAG, "create group failed") {
            createGroup(title)
        }.onFailure { failure ->
            _events.trySend(ContactsEvent.GroupActionFailed)
        }

        refreshGroups()
    }

    /** Добавляет выделенные контакты в выбранные группы. */
    suspend fun onAddSelectedToGroup(groupIds: Set<Long>) {
        val contactIds = selection.selected.value.toList()
        if (contactIds.isEmpty() || groupIds.isEmpty()) return
        safeCall(GROUPS_TAG, "add contacts to group failed") {
            groupIds.forEach { groupId -> addContactsToGroup(groupId, contactIds) }
            _events.trySend(ContactsEvent.GroupAddFinished(contactIds.size))
        }.onFailure { failure ->
            _events.trySend(ContactsEvent.GroupActionFailed)
        }

        selection.clear()
    }

    /** Перечитывает группы и членство: фильтр и пикер живут на свежих данных. */
    private suspend fun refreshGroups() {
        safeCall(GROUPS_TAG, "refresh groups failed") {
            val groups = getGroups()
            _groups.value = groups
            _membership.value = getMembership()
            // Удалённая группа не должна оставаться активным фильтром.
            if (filters.selectedGroupId.value?.let { selected -> groups.none { it.id == selected } } == true) {
                filters.onGroupMissing()
            }
        }
    }

    /** Аккаунты для фильтра и создания: сбой не мешает списку. */
    private suspend fun refreshAccounts() {
        safeCall(GROUPS_TAG, "refresh accounts failed") {
            val accounts = getAccounts()
            _accounts.value = accounts
            // Удалённый аккаунт не должен оставаться активным фильтром.
            if (filters.selectedAccount.value?.let { selected -> selected !in accounts } == true) {
                filters.onAccountMissing()
            }
        }
        safeCall(GROUPS_TAG, "refresh account sync infos failed") {
            val syncInfos = getAccountSyncInfos()
            _accountSyncInfos.value = syncInfos
        }.onFailure { failure ->
            _accountSyncInfos.value = emptyList()
        }
    }

    /** Контакт «Мой профиль» есть не на всех устройствах: ищем его при загрузке. */
    private suspend fun refreshProfile() {
        safeCall(GROUPS_TAG, "load profile failed") {
            _profileContactId.value = getProfileContactId()
        }.onFailure { failure ->
            _profileContactId.value = null
        }
    }
}
