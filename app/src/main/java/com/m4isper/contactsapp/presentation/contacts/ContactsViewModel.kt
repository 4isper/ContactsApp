package com.m4isper.contactsapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.presentation.common.Loadable
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
    val event: ContactsEvent? = null,
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()

    val isSelectionMode: Boolean
        get() = selection.isNotEmpty()
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

private data class ContactsLoad(
    val all: List<ContactDetails>,
    val loading: Boolean,
    val query: String,
    val failure: Throwable?,
)

private data class ContactsMiddle(
    val favorites: Set<Long>,
    val favoritesOnly: Boolean,
    val selection: Set<Long>,
    val window: DeleteWindow?,
    val event: ContactsEvent?,
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
    private val vCardCodec: VCardCodec,
) : ViewModel() {
    private val allContacts = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val searchQueryFlow = MutableStateFlow("")
    private val loadFailure = MutableStateFlow<Throwable?>(null)
    private val favoritesOnlyFlow = MutableStateFlow(false)
    private val selectionFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val eventFlow = MutableStateFlow<ContactsEvent?>(null)

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
                favoritesFlow,
                favoritesOnlyFlow,
                selectionFlow,
                deletes.window,
                eventFlow,
            ) { favorites, favoritesOnly, selection, deleteWindow, event ->
                ContactsMiddle(favorites, favoritesOnly, selection, deleteWindow, event)
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
            filterContacts(load.all, load.query, middle.favoritesOnly, middle.favorites)
                .filterNot { it.contactId in pending }
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
                } catch (_: Exception) {
                    emptyList()
                }
            eventFlow.value = ContactsEvent.Share(vCardCodec.encode(contacts))
        }
    }

    fun onEventHandled() {
        eventFlow.value = null
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
                loadFailure.value = e
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clear() {
        allContacts.value = emptyList()
        selectionFlow.value = emptySet()
    }
}
