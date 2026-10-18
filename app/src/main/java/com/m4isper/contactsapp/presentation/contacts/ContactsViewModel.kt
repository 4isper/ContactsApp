package com.m4isper.contactsapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.logw
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    /** Импорт: в файле не нашлось ни одной vCard. */
    data object ImportEmpty : ContactsEvent

    /** Импорт: создано [count] контактов. */
    data class ImportFinished(
        val count: Int,
    ) : ContactsEvent

    /** Импорт: ни один контакт не создан из-за ошибок. */
    data object ImportFailed : ContactsEvent

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

private data class ContactsGroupsMiddle(
    val groups: List<ContactGroup>,
    val selectedGroupId: Long?,
    val membership: Map<Long, Set<Long>>,
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
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
// Подавление осознанное: фасад сценариев списка — тонкие делегаты в координаторы
// (удаление, файлы); декомпозиция по файлам не даёт выгоды, а лимит функций уже
// перешагнут законченными сценариями (поиск, выделение, избранное, обновление).
@Suppress("TooManyFunctions")
class ContactsViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val getContactDetailsUseCase: GetContactDetailsUseCase,
    deleteContactsUseCase: DeleteContactsUseCase,
    importContactsUseCase: ImportContactsUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getGroupMembershipUseCase: GetGroupMembershipUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val addContactsToGroupUseCase: AddContactsToGroupUseCase,
    private val vCardCodec: VCardCodec,
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
                combine(groupsFlow, selectedGroupFlow, membershipFlow, ::ContactsGroupsMiddle),
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
                )
            },
        ) { load, middle -> contactsUiState(load, middle) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = ContactsUiState(),
            )

    private fun contactsUiState(
        load: ContactsLoad,
        middle: ContactsMiddle,
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
            ).filterNot { it.contactId in pending }
        val content =
            when {
                load.failure != null -> Loadable.Error(load.failure.message)
                load.loading && visible.isEmpty() -> Loadable.Loading
                visible.isEmpty() -> Loadable.Empty
                else -> Loadable.Content(visible)
            }
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
            event = middle.event,
        )
    }

    init {
        observeDeleteOutcome()
    }

    /** Импорт и экспорт .vcf: сценарии вынесены в [ContactsFileActions]. */
    internal val files =
        ContactsFileActions(
            scope = viewModelScope,
            events = eventFlow,
            getContacts = { getContactsUseCase() },
            getContactDetails = { getContactDetailsUseCase(it) },
            importContacts = importContactsUseCase,
            vCardCodec = vCardCodec,
            refresh = ::refresh,
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

    /** Тап по карточке в режиме выделения и long-press в любом режиме переключают выделение. */
    fun onContactSelected(contactId: Long) {
        val current = selectionFlow.value
        selectionFlow.value = if (contactId in current) current - contactId else current + contactId
    }

    fun onClearSelection() {
        selectionFlow.value = emptySet()
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

    fun onImportText(text: String) = files.onImportText(text)

    fun onExportRequested() = files.onExportRequested()

    /** Догружает полные детали выделенных контактов: в списке они облегчённые, для vCard этого мало. */
    @Suppress("TooGenericExceptionCaught")
    fun onShareSelected() {
        val ids = selectionFlow.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val contacts =
                try {
                    ids.mapNotNull { getContactDetailsUseCase(it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logw(LIST_TAG, "share vCard encode failed", e)
                    emptyList()
                }
            eventFlow.value = ContactsEvent.Share(vCardCodec.encode(contacts))
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

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            loadFailure.value = null
            selectionFlow.value = emptySet()
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
    }
}
