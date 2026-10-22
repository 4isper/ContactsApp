package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.GetProfileContactIdUseCase
import com.m4isper.contactsapp.presentation.common.logw
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
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
        _profileContactId.value = null
    }

    /** Создаёт группу из фильтра: список групп и членство перечитываются. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: неудачное создание группы показывается тостом.
    suspend fun onCreateGroup(title: String) {
        if (title.isBlank()) return
        try {
            createGroup(title)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(GROUPS_TAG, "create group failed", e)
            _events.trySend(ContactsEvent.GroupActionFailed)
        }
        refreshGroups()
    }

    /** Добавляет выделенные контакты в выбранные группы. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: неудачное добавление в группу показывается тостом.
    suspend fun onAddSelectedToGroup(groupIds: Set<Long>) {
        val contactIds = selection.selected.value.toList()
        if (contactIds.isEmpty() || groupIds.isEmpty()) return
        try {
            groupIds.forEach { groupId -> addContactsToGroup(groupId, contactIds) }
            _events.trySend(ContactsEvent.GroupAddFinished(contactIds.size))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(GROUPS_TAG, "add contacts to group failed", e)
            _events.trySend(ContactsEvent.GroupActionFailed)
        }
        selection.clear()
    }

    /** Перечитывает группы и членство: фильтр и пикер живут на свежих данных. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой списка групп не мешает списку контактов.
    private suspend fun refreshGroups() {
        try {
            val groups = getGroups()
            _groups.value = groups
            _membership.value = getMembership()
            // Удалённая группа не должна оставаться активным фильтром.
            if (filters.selectedGroupId.value?.let { selected -> groups.none { it.id == selected } } == true) {
                filters.onGroupMissing()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(GROUPS_TAG, "refresh groups failed", e)
        }
    }

    /** Аккаунты для фильтра и создания: сбой не мешает списку. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой чтения аккаунтов не мешает списку.
    private suspend fun refreshAccounts() {
        try {
            val accounts = getAccounts()
            _accounts.value = accounts
            // Удалённый аккаунт не должен оставаться активным фильтром.
            if (filters.selectedAccount.value?.let { selected -> selected !in accounts } == true) {
                filters.onAccountMissing()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(GROUPS_TAG, "refresh accounts failed", e)
        }
    }

    /** Контакт «Мой профиль» есть не на всех устройствах: ищем его при загрузке. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: отсутствие профиля не мешает списку.
    private suspend fun refreshProfile() {
        try {
            _profileContactId.value = getProfileContactId()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(GROUPS_TAG, "load profile failed", e)
            _profileContactId.value = null
        }
    }
}
