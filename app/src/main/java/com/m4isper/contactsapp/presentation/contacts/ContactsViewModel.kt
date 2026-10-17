package com.m4isper.contactsapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
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
    val event: ContactsEvent? = null,
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()

    val isSelectionMode: Boolean
        get() = selection.isNotEmpty()
}

/** Одноразовое событие экрана: UI обрабатывает его и сбрасывает через [ContactsViewModel.onEventHandled]. */
sealed interface ContactsEvent {
    /** Подготовленные к vCard-шарингу контакты из выделения. */
    data class Share(
        val contacts: List<ContactDetails>,
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

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactsViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val getContactDetailsUseCase: GetContactDetailsUseCase,
    private val deleteContactsUseCase: DeleteContactsUseCase,
    importContactsUseCase: ImportContactsUseCase,
) : ViewModel() {
    private val allContacts = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val searchQueryFlow = MutableStateFlow("")
    private val loadFailure = MutableStateFlow<Throwable?>(null)
    private val favoritesOnlyFlow = MutableStateFlow(false)
    private val selectionFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val eventFlow = MutableStateFlow<ContactsEvent?>(null)

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
            favoritesFlow,
            favoritesOnlyFlow,
            selectionFlow,
            eventFlow,
        ) { load, favorites, favoritesOnly, selection, event ->
            val visible = filterContacts(load.all, load.query, favoritesOnly, favorites)
            val content =
                when {
                    load.failure != null -> Loadable.Error(load.failure.message)
                    load.loading && visible.isEmpty() -> Loadable.Loading
                    visible.isEmpty() -> Loadable.Empty
                    else -> Loadable.Content(visible)
                }
            ContactsUiState(
                searchQuery = load.query,
                showFavoritesOnly = favoritesOnly,
                favorites = favorites,
                isRefreshing = load.loading,
                content = content,
                selection = selection,
                event = event,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = ContactsUiState(),
        )

    /** Импорт и экспорт .vcf: сценарии вынесены в [ContactsFileActions]. */
    internal val files =
        ContactsFileActions(
            scope = viewModelScope,
            events = eventFlow,
            getContacts = { getContactsUseCase() },
            getContactDetails = { getContactDetailsUseCase(it) },
            importContacts = importContactsUseCase,
            refresh = ::refresh,
        )

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

    // Catch-all осознанный: ошибку удаления показываем одноразовым событием, список не меняем.
    @Suppress("TooGenericExceptionCaught")
    fun onDeleteSelected() {
        val ids = selectionFlow.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                deleteContactsUseCase(ids)
                allContacts.value = allContacts.value.filterNot { it.contactId in ids }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                eventFlow.value = ContactsEvent.DeleteFailed
            } finally {
                selectionFlow.value = emptySet()
            }
        }
    }

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
            eventFlow.value = ContactsEvent.Share(contacts)
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
