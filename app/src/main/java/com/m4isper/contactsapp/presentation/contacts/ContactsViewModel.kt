package com.m4isper.contactsapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.contacts.filterContacts
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.usecase.contacts.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.safeCall
import com.m4isper.contactsapp.presentation.contacts.effects.ContactsFileActions
import com.m4isper.contactsapp.presentation.contacts.effects.DeleteWindow
import com.m4isper.contactsapp.presentation.contacts.effects.PendingDeleteCoordinator
import com.m4isper.contactsapp.presentation.contacts.state.ContactsFavoriteState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsFilterState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsGroupState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsListState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsSelectionState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
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
        load.failure != null -> Loadable.Error(AppError.from(load.failure))
        load.loading && visible.isEmpty() -> Loadable.Loading
        visible.isEmpty() -> Loadable.Empty
        else -> Loadable.Content(visible)
    }

/** Собирает состояние экрана из частей: загрузка, середина и настройки. */
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

/** Перечитывает список и связанные с ним группы и избранное. */
private suspend fun reloadScreen(
    list: ContactsListState,
    groups: ContactsGroupState,
    favorites: ContactsFavoriteState,
) {
    list.load()
    groups.refresh()
    favorites.refresh()
}

/** События сценариев групп показываются пользователю как события экрана. */
private suspend fun observeGroupEvents(
    groups: ContactsGroupState,
    events: MutableStateFlow<ContactsEvent?>,
) {
    groups.events.collect { event -> events.value = event }
}

/**
 * Итоги отложенного удаления: успевшие удалиться контакты убираем из локального списка
 * и перечитываем провайдера; не удалённые честно остаются в списке с тостом об ошибке.
 */
private suspend fun observeDeleteOutcome(
    deletes: PendingDeleteCoordinator,
    list: ContactsListState,
    events: MutableStateFlow<ContactsEvent?>,
    refresh: () -> Unit,
) {
    deletes.finished.collect { finished ->
        if (finished != null) {
            list.remove(finished.deletedIds)
            deletes.finishedHandled()
            if (!finished.allDeleted) {
                events.value = ContactsEvent.DeleteFailed
            }
            refresh()
        }
    }
    deletes.failure.collect { failed ->
        if (failed) {
            deletes.failureHandled()
            events.value = ContactsEvent.DeleteFailed
        }
    }
}

/**
 * Сценарии списка контактов: фасад над частями состояния экрана.
 * Загрузка, фильтры, выделение, избранное и группы живут в своих кусках,
 * здесь только состояние экрана и действия, которые их связывают.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class ContactsViewModel(
    val list: ContactsListState,
    val filters: ContactsFilterState,
    val selection: ContactsSelectionState,
    val favorites: ContactsFavoriteState,
    val groups: ContactsGroupState,
    deleteContactsUseCase: DeleteContactsUseCase,
    exportContactsUseCase: ExportContactsUseCase,
    getContactsUseCase: GetContactsUseCase,
    observeSettingsUseCase: ObserveSettingsUseCase,
) : ViewModel() {
    private val eventFlow = MutableStateFlow<ContactsEvent?>(null)
    private var changesJob: Job? = null

    /** Отложенное удаление: окно отмены живёт в [PendingDeleteCoordinator]. */
    internal val deletes =
        PendingDeleteCoordinator(
            scope = viewModelScope,
            deleteContacts = deleteContactsUseCase,
        )

    /** Экспорт .vcf: сценарии вынесены в [ContactsFileActions]. */
    internal val files =
        ContactsFileActions(
            scope = viewModelScope,
            events = eventFlow,
            exportContacts = exportContactsUseCase,
            getContacts = { getContactsUseCase() },
        )

    private val favoritesFlow: StateFlow<Set<Long>> =
        favorites.favorites
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
            combine(
                list.contacts,
                list.isLoading,
                filters.searchQuery,
                list.failure,
                ::ContactsLoad,
            ),
            combine(
                combine(
                    favoritesFlow,
                    filters.favoritesOnly,
                    selection.selected,
                    deletes.window,
                    eventFlow,
                    ::ContactsMiddleBase,
                ),
                combine(groups.groups, filters.selectedGroupId, groups.membership, ::GroupsState),
                combine(groups.accounts, filters.selectedAccount, groups.profileContactId, ::AccountsState),
            ) { base, groups, accounts ->
                ContactsMiddle(
                    favorites = base.favorites,
                    favoritesOnly = base.favoritesOnly,
                    selection = base.selection,
                    window = base.window,
                    event = base.event,
                    groups = groups.groups,
                    selectedGroupId = groups.selectedGroupId,
                    membership = groups.membership,
                    accounts = accounts.accounts,
                    selectedAccount = accounts.selectedAccount,
                    profileContactId = accounts.profileContactId,
                )
            },
            settingsFlow,
        ) { load, middle, settings -> contactsUiState(load, middle, settings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = ContactsUiState(),
            )

    init {
        viewModelScope.launch { observeGroupEvents(groups, eventFlow) }
        viewModelScope.launch { observeDeleteOutcome(deletes, list, eventFlow) { refresh() } }
    }

    /** Удаляет выделенные контакты: окно отмены показывает snackbar. */
    fun onDeleteSelected() {
        val ids = selection.selected.value
        if (ids.isEmpty()) return
        selection.clear()
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

    /** Экспорт выделенных контактов в .vcf. */
    fun onExportSelection() = files.onExportSelection(selection.selected.value.toList())

    /** Догружает полные детали выделенных контактов: в списке они облегчённые, для vCard этого мало. */
    suspend fun onShareSelected() {
        val ids = selection.selected.value.toList()
        if (ids.isEmpty()) return
        val content =
            safeCall(LIST_TAG, "share vCard encode failed") {
                files.exportContent(ids)
            }.getOrElse { null }

        if (content != null) {
            eventFlow.value = ContactsEvent.Share(content)
        }
    }

    /**
     * Пакетное избранное: если выбраны только избранные — снимаем отметку,
     * иначе добавляем все ещё не избранные контакты.
     */
    suspend fun onToggleSelectedFavorite() {
        favorites.onToggleSelectedFavorite(selection.selected.value, favoritesFlow.value)
    }

    fun onEventHandled() {
        eventFlow.value = null
    }

    /** Перечитывает список и связанные с ним группы, аккаунты и избранное. */
    fun refresh() {
        selection.clear()
        viewModelScope.launch { reloadScreen(list, groups, favorites) }
    }

    /** Очищает список: экран закрыт, состояние не нужно. */
    fun clear() {
        list.clear()
        filters.onSearchQueryChange("")
        filters.resetFavoritesOnly()
        selection.clear()
        groups.clear()
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
                list.providerChanges
                    .debounce(CONTACTS_CHANGE_DEBOUNCE_MILLIS)
                    .collect { viewModelScope.launch { reloadScreen(list, groups, favorites) } }
            }
    }

    fun stopObservingChanges() {
        changesJob?.cancel()
        changesJob = null
    }
}
