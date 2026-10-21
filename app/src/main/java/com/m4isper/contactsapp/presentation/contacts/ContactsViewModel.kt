package com.m4isper.contactsapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.repository.ContactsChangeSource
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.GetProfileContactIdUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.RefreshFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.logw
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsUiState(
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val favorites: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
    val content: Loadable<List<ContactDetails>> = Loadable.Loading,
    val selection: Set<Long> = emptySet(),
    val pendingDeleteIds: Set<Long> = emptySet(),
    val deleteClosing: Boolean = false,
    val groups: List<ContactGroup> = emptyList(),
    val selectedGroupId: Long? = null,
    val accounts: List<ContactAccount> = emptyList(),
    val selectedAccount: ContactAccount? = null,
    val sortOrder: ContactSortOrder = ContactSortOrder.BY_NAME,
    val listDensity: ListDensity = ListDensity.COMFORTABLE,
    val pinFavorites: Boolean = false,
    /** id контакта «Мой профиль»; null, если профиля нет. */
    val profileContactId: Long? = null,
    val event: ContactsEvent? = null,
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()

    val isSelectionMode: Boolean
        get() = selection.isNotEmpty()

    val selectedGroup: ContactGroup?
        get() = groups.firstOrNull { it.id == selectedGroupId }
}

/** Одноразовое событие экрана: UI обрабатывает его и сбрасывает через [ContactsViewModel.onEventHandled]. */
sealed interface ContactsEvent {
    /** Готовый vCard-текст выделенных контактов для шаринга. */
    data class Share(
        val content: String,
    ) : ContactsEvent

    /** Не удалось удалить выделенные контакты. */
    data object DeleteFailed : ContactsEvent

    /** В группу добавлены выделенные контакты. */
    data class GroupAddFinished(
        val count: Int,
    ) : ContactsEvent

    /** Не удалось создать группу или добавить в неё контакты. */
    data object GroupActionFailed : ContactsEvent

    /** Экспорт: [content] — готовый .vcf на [count] контактов. */
    data class ExportReady(
        val content: String,
        val count: Int,
    ) : ContactsEvent

    /** Экспорт: экспортировать нечего. */
    data object ExportEmpty : ContactsEvent

    /** Экспорт: не удалось собрать контакты. */
    data object ExportFailed : ContactsEvent
}

internal const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L

internal const val CONTACTS_CHANGE_DEBOUNCE_MILLIS = 500L

private const val LIST_TAG = "ContactsList"

private data class ContactsLoad(
    val all: List<ContactDetails>,
    val loading: Boolean,
    val query: String,
    val failure: Throwable?,
)

private data class ContactsMiddleBase(
    val favorites: Set<Long>,
    val favoritesOnly: Boolean,
    val selection: Set<Long>,
    val window: DeleteWindow?,
    val event: ContactsEvent?,
)

/** Группы и членство: часть состояния списка. */
private data class GroupsState(
    val groups: List<ContactGroup>,
    val selectedGroupId: Long?,
    val membership: Map<Long, Set<Long>>,
)

/** Аккаунты и профиль: часть состояния списка. */
private data class AccountsState(
    val accounts: List<ContactAccount>,
    val selectedAccount: ContactAccount?,
    val profileContactId: Long?,
)

private data class ContactsGroupsMiddle(
    val groups: List<ContactGroup>,
    val selectedGroupId: Long?,
    val membership: Map<Long, Set<Long>>,
    val accounts: List<ContactAccount>,
    val selectedAccount: ContactAccount?,
    val profileContactId: Long?,
)

private data class ContactsMiddle(
    val favorites: Set<Long>,
    val favoritesOnly: Boolean,
    val selection: Set<Long>,
    val window: DeleteWindow?,
    val event: ContactsEvent?,
    val groups: List<ContactGroup>,
    val selectedGroupId: Long?,
    val membership: Map<Long, Set<Long>>,
    val accounts: List<ContactAccount>,
    val selectedAccount: ContactAccount?,
    val profileContactId: Long?,
)

private fun contactsContent(
    load: ContactsLoad,
    visible: List<ContactDetails>,
): Loadable<List<ContactDetails>> =
    when {
        load.failure != null -> Loadable.Error(load.failure.message)
        load.loading && visible.isEmpty() -> Loadable.Loading
        visible.isEmpty() -> Loadable.Empty
        else -> Loadable.Content(visible)
    }

private fun shouldPinFavorites(
    load: ContactsLoad,
    middle: ContactsMiddle,
    settings: AppSettings,
): Boolean =
    settings.favoritesFirst &&
        !middle.favoritesOnly &&
        load.query.isBlank() &&
        middle.selectedGroupId == null &&
        middle.selectedAccount == null

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
// Подавление осознанное: фасад сценариев списка — тонкие делегаты в координаторы
// (удаление, файлы); декомпозиция по файлам не даёт выгоды, а лимит функций уже
// перешагнут законченными сценариями (поиск, выделение, избранное, обновление).
@Suppress("TooManyFunctions")
class ContactsViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    private val getContactAccountsUseCase: GetContactAccountsUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val getContactDetailsUseCase: GetContactDetailsUseCase,
    deleteContactsUseCase: DeleteContactsUseCase,
    private val exportContactsUseCase: ExportContactsUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getGroupMembershipUseCase: GetGroupMembershipUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val addContactsToGroupUseCase: AddContactsToGroupUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val refreshFavoritesUseCase: RefreshFavoritesUseCase,
    private val getProfileContactIdUseCase: GetProfileContactIdUseCase,
    private val contactsChangeSource: ContactsChangeSource,
) : ViewModel() {
    private val allContacts = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val searchQueryFlow = MutableStateFlow("")
    private val loadFailure = MutableStateFlow<Throwable?>(null)
    private val favoritesOnlyFlow = MutableStateFlow(false)
    private val selectionFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val eventFlow = MutableStateFlow<ContactsEvent?>(null)
    private val groupsFlow = MutableStateFlow<List<ContactGroup>>(emptyList())
    private val selectedGroupFlow = MutableStateFlow<Long?>(null)
    private val membershipFlow = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    private val accountsFlow = MutableStateFlow<List<ContactAccount>>(emptyList())
    private val profileContactIdFlow = MutableStateFlow<Long?>(null)
    private val selectedAccountFlow = MutableStateFlow<ContactAccount?>(null)
    private var changesJob: Job? = null
    private var pendingProviderRefresh = false

    /** Отложенное удаление: окно отмены живёт в [PendingDeleteCoordinator]. */
    internal val deletes =
        PendingDeleteCoordinator(
            scope = viewModelScope,
            deleteContacts = deleteContactsUseCase,
        )

    private val favoritesFlow: StateFlow<Set<Long>> =
        observeFavoritesUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = emptySet(),
            )

    private val settingsFlow: StateFlow<AppSettings> =
        observeSettingsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = AppSettings(),
            )

    val uiState: StateFlow<ContactsUiState> =
        combine(
            combine(allContacts, isLoading, searchQueryFlow, loadFailure, ::ContactsLoad),
            combine(
                combine(
                    favoritesFlow,
                    favoritesOnlyFlow,
                    selectionFlow,
                    deletes.window,
                    eventFlow,
                    ::ContactsMiddleBase,
                ),
                combine(
                    combine(groupsFlow, selectedGroupFlow, membershipFlow, ::GroupsState),
                    combine(accountsFlow, selectedAccountFlow, profileContactIdFlow, ::AccountsState),
                ) { groups, accounts ->
                    ContactsGroupsMiddle(
                        groups = groups.groups,
                        selectedGroupId = groups.selectedGroupId,
                        membership = groups.membership,
                        accounts = accounts.accounts,
                        selectedAccount = accounts.selectedAccount,
                        profileContactId = accounts.profileContactId,
                    )
                },
            ) { base, groups ->
                ContactsMiddle(
                    favorites = base.favorites,
                    favoritesOnly = base.favoritesOnly,
                    selection = base.selection,
                    window = base.window,
                    event = base.event,
                    groups = groups.groups,
                    selectedGroupId = groups.selectedGroupId,
                    membership = groups.membership,
                    accounts = groups.accounts,
                    selectedAccount = groups.selectedAccount,
                    profileContactId = groups.profileContactId,
                )
            },
            settingsFlow,
        ) { load, middle, settings -> contactsUiState(load, middle, settings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = ContactsUiState(),
            )

    private fun contactsUiState(
        load: ContactsLoad,
        middle: ContactsMiddle,
        settings: AppSettings,
    ): ContactsUiState {
        val pending = middle.window?.contactIds.orEmpty()
        val visible =
            filterContacts(
                contacts = load.all,
                query = load.query,
                favoritesOnly = middle.favoritesOnly,
                favorites = middle.favorites,
                selectedGroupId = middle.selectedGroupId,
                membership = middle.membership,
                groups = middle.groups,
                selectedAccount = middle.selectedAccount,
            ).filterNot { it.contactId in pending }
        val content = contactsContent(load, visible)
        return ContactsUiState(
            searchQuery = load.query,
            showFavoritesOnly = middle.favoritesOnly,
            favorites = middle.favorites,
            isRefreshing = load.loading,
            content = content,
            selection = middle.selection,
            pendingDeleteIds = pending,
            deleteClosing = middle.window?.closing == true,
            groups = middle.groups,
            selectedGroupId = middle.selectedGroupId,
            accounts = middle.accounts,
            selectedAccount = middle.selectedAccount,
            sortOrder = settings.sortOrder,
            listDensity = settings.listDensity,
            pinFavorites = shouldPinFavorites(load, middle, settings),
            profileContactId = middle.profileContactId,
            event = middle.event,
        )
    }

    init {
        observeDeleteOutcome()
    }

    /** Экспорт .vcf: сценарии вынесены в [ContactsFileActions]. */
    internal val files =
        ContactsFileActions(
            scope = viewModelScope,
            events = eventFlow,
            exportContacts = exportContactsUseCase,
            getContacts = { getContactsUseCase() },
        )

    /**
     * Итоги отложенного удаления: успевшие удалиться контакты убираем из локального списка
     * и перечитываем провайдера; не удалённые честно остаются в списке с тостом об ошибке.
     */
    private fun observeDeleteOutcome() {
        viewModelScope.launch {
            deletes.finished.collect { finished ->
                if (finished != null) {
                    allContacts.value = allContacts.value.filterNot { it.contactId in finished.deletedIds }
                    deletes.finishedHandled()
                    if (!finished.allDeleted) {
                        eventFlow.value = ContactsEvent.DeleteFailed
                    }
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            deletes.failure.collect { failed ->
                if (failed) {
                    deletes.failureHandled()
                    eventFlow.value = ContactsEvent.DeleteFailed
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        selectionFlow.value = emptySet()
        searchQueryFlow.value = query
    }

    fun onToggleFavoritesOnly() {
        selectionFlow.value = emptySet()
        favoritesOnlyFlow.value = !favoritesOnlyFlow.value
    }

    /** Фильтр по группе; null — показать все контакты. */
    fun onGroupSelected(groupId: Long?) {
        selectionFlow.value = emptySet()
        selectedGroupFlow.value = groupId
    }

    /** Фильтр по аккаунту; null — показать контакты всех аккаунтов. */
    fun onAccountSelected(account: ContactAccount?) {
        selectionFlow.value = emptySet()
        selectedAccountFlow.value = account
    }

    /** Тап по карточке в режиме выделения и long-press в любом режиме переключают выделение. */
    fun onContactSelected(contactId: Long) {
        val current = selectionFlow.value
        selectionFlow.value = if (contactId in current) current - contactId else current + contactId
    }

    fun onClearSelection() {
        selectionFlow.value = emptySet()
    }

    /** Быстрое избранное из строки списка. */
    @Suppress("TooGenericExceptionCaught")
    fun onToggleFavorite(contactId: Long) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(contactId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "toggle favorite failed", e)
            }
        }
    }

    /**
     * Пакетное избранное: если выбраны только избранные — снимаем отметку,
     * иначе добавляем все ещё не избранные контакты.
     */
    @Suppress("TooGenericExceptionCaught")
    fun onToggleSelectedFavorite() {
        val ids = selectionFlow.value
        if (ids.isEmpty()) return
        val favorites = favoritesFlow.value
        val allFavorite = ids.all { it in favorites }
        val toRemove = if (allFavorite) ids else emptySet()
        val toAdd = if (allFavorite) emptySet() else ids - favorites
        viewModelScope.launch {
            try {
                toRemove.forEach { removeFavoriteUseCase(it) }
                toAdd.forEach { toggleFavoriteUseCase(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "batch favorite update failed", e)
            }
        }
    }

    /** Повторный вызов «выбрать все» снимает выделение. */
    fun onSelectAll(contactIds: List<Long>) {
        val all = contactIds.toSet()
        selectionFlow.value = if (selectionFlow.value.containsAll(all)) emptySet() else all
    }

    fun onDeleteSelected() {
        val ids = selectionFlow.value
        if (ids.isEmpty()) return
        selectionFlow.value = emptySet()
        deletes.stage(ids)
    }

    /** Отмена отложенного удаления: контакты возвращаются в список. */
    fun onUndoDelete() {
        deletes.undo()
    }

    /** Snackbar закрыт — окно удаления завершено. */
    fun onDeleteWindowClosed() {
        deletes.windowClosed()
    }

    fun onExportRequested() = files.onExportRequested()

    /** Экспорт выделенных контактов в .vcf. */
    fun onExportSelection() = files.onExportSelection(selectionFlow.value.toList())

    /** Догружает полные детали выделенных контактов: в списке они облегчённые, для vCard этого мало. */
    @Suppress("TooGenericExceptionCaught")
    fun onShareSelected() {
        val ids = selectionFlow.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val content =
                try {
                    exportContactsUseCase(ids)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logw(LIST_TAG, "share vCard encode failed", e)
                    null
                }
            if (content != null) {
                eventFlow.value = ContactsEvent.Share(content)
            }
        }
    }

    fun onEventHandled() {
        eventFlow.value = null
    }

    /** Создаёт группу из фильтра: список групп и членство перечитываются. */
    @Suppress("TooGenericExceptionCaught")
    fun onCreateGroup(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                createGroupUseCase(title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "create group failed", e)
                eventFlow.value = ContactsEvent.GroupActionFailed
            }
            refreshGroups()
        }
    }

    /** Добавляет выделенные контакты в выбранные группы. */
    @Suppress("TooGenericExceptionCaught")
    fun onAddSelectedToGroup(groupIds: Set<Long>) {
        val contactIds = selectionFlow.value.toList()
        if (contactIds.isEmpty() || groupIds.isEmpty()) return
        viewModelScope.launch {
            try {
                groupIds.forEach { groupId -> addContactsToGroupUseCase(groupId, contactIds) }
                eventFlow.value = ContactsEvent.GroupAddFinished(contactIds.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "add contacts to group failed", e)
                eventFlow.value = ContactsEvent.GroupActionFailed
            }
            selectionFlow.value = emptySet()
            refresh()
        }
    }

    /** Перечитывает группы и членство: фильтр и пикер живут на свежих данных. */
    @Suppress("TooGenericExceptionCaught")
    private fun refreshGroups() {
        viewModelScope.launch {
            try {
                val groups = getGroupsUseCase()
                groupsFlow.value = groups
                membershipFlow.value = getGroupMembershipUseCase()
                // Удалённая группа не должна оставаться активным фильтром.
                if (selectedGroupFlow.value?.let { selected -> groups.none { it.id == selected } } == true) {
                    selectedGroupFlow.value = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "refresh groups failed", e)
            }
        }
    }

    /** Контакт «Мой профиль» есть не на всех устройствах: ищем его при загрузке. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: отсутствие профиля не мешает списку.
    private fun refreshProfile() {
        viewModelScope.launch {
            try {
                profileContactIdFlow.value = getProfileContactIdUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "load profile failed", e)
                profileContactIdFlow.value = null
            }
        }
    }

    /** Избранное читается из провайдера: перечитываем его вместе с контактами. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой чтения избранного не мешает списку.
    private fun refreshFavorites() {
        viewModelScope.launch {
            try {
                refreshFavoritesUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "refresh favorites failed", e)
            }
        }
    }

    /** Аккаунты для фильтра и создания: сбой не мешает списку. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой чтения аккаунтов не мешает списку.
    private fun refreshAccounts() {
        viewModelScope.launch {
            try {
                val accounts = getContactAccountsUseCase()
                accountsFlow.value = accounts
                // Удалённый аккаунт не должен оставаться активным фильтром.
                if (selectedAccountFlow.value?.let { selected -> selected !in accounts } == true) {
                    selectedAccountFlow.value = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(LIST_TAG, "refresh accounts failed", e)
            }
        }
    }

    /**
     * Подписка на изменения провайдера. Вызывается со стороны экрана и живёт
     * только пока список видим, чтобы не держать ContentObserver в фоне.
     */
    @OptIn(FlowPreview::class)
    fun startObservingChanges() {
        if (changesJob?.isActive == true) return
        changesJob =
            viewModelScope.launch {
                contactsChangeSource
                    .observeChanges()
                    .debounce(CONTACTS_CHANGE_DEBOUNCE_MILLIS)
                    .collect { refreshSilently() }
            }
    }

    fun stopObservingChanges() {
        changesJob?.cancel()
        changesJob = null
        pendingProviderRefresh = false
    }

    /** Фоновое обновление сохраняет выделение: пользователь не теряет контекст. */
    private fun refreshSilently() {
        launchLoad(clearSelection = false)
    }

    fun refresh() {
        launchLoad(clearSelection = true)
    }

    private fun launchLoad(clearSelection: Boolean) {
        if (isLoading.value) {
            if (!clearSelection) pendingProviderRefresh = true
            return
        }
        isLoading.value = true
        viewModelScope.launch {
            loadContacts(clearSelection)
        }
    }

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadContacts(clearSelection: Boolean) {
        loadFailure.value = null
        if (clearSelection) selectionFlow.value = emptySet()
        try {
            allContacts.value = getContactsUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(LIST_TAG, "refresh contacts failed", e)
            loadFailure.value = e
        } finally {
            isLoading.value = false
        }
        refreshGroups()
        refreshAccounts()
        refreshFavorites()
        refreshProfile()
        if (pendingProviderRefresh) {
            pendingProviderRefresh = false
            launchLoad(clearSelection = false)
        }
    }

    fun clear() {
        allContacts.value = emptyList()
        searchQueryFlow.value = ""
        favoritesOnlyFlow.value = false
        loadFailure.value = null
        selectionFlow.value = emptySet()
        groupsFlow.value = emptyList()
        membershipFlow.value = emptyMap()
        selectedGroupFlow.value = null
        accountsFlow.value = emptyList()
        selectedAccountFlow.value = null
    }
}
