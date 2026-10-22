package com.m4isper.contactsapp.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.AppError
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.usecase.CopyContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.MoveContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.safeCall
import com.m4isper.contactsapp.presentation.contacts.SUBSCRIBE_TIMEOUT_MILLIS
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DETAILS_TAG = "ContactDetails"

data class DetailsUiState(
    val content: Loadable<ContactDetails> = Loadable.Loading,
    val isFavorite: Boolean = false,
    val groups: List<ContactGroup> = emptyList(),
    /** Аккаунты, в которые можно скопировать или перенести контакт. */
    val accounts: List<ContactAccount> = emptyList(),
    val event: DetailsEvent? = null,
)

/** Одноразовые события карточки контакта. */
sealed interface DetailsEvent {
    /** Контакт скопирован в другой аккаунт. */
    data class ContactCopied(
        val contactId: Long,
    ) : DetailsEvent

    /** Контакт перенесён в другой аккаунт, исходный удалён. */
    data class ContactMoved(
        val contactId: Long,
    ) : DetailsEvent

    /** Не удалось скопировать или перенести контакт. */
    data object AccountActionFailed : DetailsEvent
}

/** Загрузка карточки: контакт, признак загрузки и ошибка. */
private data class DetailsLoad(
    val details: ContactDetails?,
    val loading: Boolean,
    val failure: Throwable?,
)

/** Прочее состояние карточки: избранное, группы, аккаунты и событие. */
private data class DetailsExtra(
    val favorites: Set<Long>,
    val groups: List<ContactGroup>,
    val accounts: List<ContactAccount>,
    val event: DetailsEvent?,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactDetailsViewModel(
    private val getContactDetailsUseCase: GetContactDetailsUseCase,
    private val getContactGroupsUseCase: GetContactGroupsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val exportContacts: ExportContactsUseCase,
    private val getContactAccountsUseCase: GetContactAccountsUseCase,
    private val copyContactToAccountUseCase: CopyContactToAccountUseCase,
    private val moveContactToAccountUseCase: MoveContactToAccountUseCase,
) : ViewModel() {
    private val selectedContactId = MutableStateFlow<Long?>(null)
    private val loadedDetails = MutableStateFlow<ContactDetails?>(null)
    private val groupsFlow = MutableStateFlow<List<ContactGroup>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val loadFailure = MutableStateFlow<Throwable?>(null)
    private val accountsFlow = MutableStateFlow<List<ContactAccount>>(emptyList())
    private val eventFlow = MutableStateFlow<DetailsEvent?>(null)

    private val favoritesFlow: StateFlow<Set<Long>> =
        observeFavoritesUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = emptySet(),
            )

    val uiState: StateFlow<DetailsUiState> =
        combine(
            combine(loadedDetails, isLoading, loadFailure, ::DetailsLoad),
            combine(favoritesFlow, groupsFlow, accountsFlow, eventFlow, ::DetailsExtra),
        ) { load, extra ->
            val content =
                when {
                    load.failure != null -> Loadable.Error(AppError.from(load.failure))
                    load.loading && load.details == null -> Loadable.Loading
                    load.details == null -> Loadable.Empty
                    else -> Loadable.Content(load.details)
                }
            DetailsUiState(
                content = content,
                isFavorite = load.details?.contactId in extra.favorites,
                groups = extra.groups,
                accounts = extra.accounts,
                event = extra.event,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = DetailsUiState(),
        )

    fun load(contactId: Long) {
        if (selectedContactId.value == contactId && (loadedDetails.value != null || isLoading.value)) return
        selectedContactId.value = contactId
        loadedDetails.value = null
        groupsFlow.value = emptyList()
        refresh()
    }

    // Catch-all осознанный: сбой записи избранного не должен ронять экран.
    fun toggleFavorite() {
        val contactId = selectedContactId.value ?: return
        viewModelScope.launch {
            safeCall(DETAILS_TAG, "toggleFavorite failed") {
                toggleFavoriteUseCase(contactId)
            }
        }
    }

    /**
     * Готовый vCard-текст загруженного контакта для share-интента; null, пока контакт не загружен.
     * suspend: сборка текста уходит на фоновый диспетчер.
     */
    suspend fun vCardText(): String? {
        val contactId = selectedContactId.value ?: return null
        return exportContacts(listOf(contactId)).takeIf { it.isNotEmpty() }
    }

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    fun refresh() {
        val contactId = selectedContactId.value ?: return
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            loadFailure.value = null
            safeCall(DETAILS_TAG, "load contact details failed") {
                val details = getContactDetailsUseCase(contactId)
                loadedDetails.value = details
                groupsFlow.value = if (details != null) loadGroups(contactId) else emptyList()
                accountsFlow.value = loadAccounts()
            }.onFailure { failure ->
                loadFailure.value = failure
            }
            isLoading.value = false
        }
    }

    /** Сбой чтения групп не отменяет показ контакта: детали важнее состава групп. */
    private suspend fun loadGroups(contactId: Long): List<ContactGroup> =
        safeCall(DETAILS_TAG, "load groups failed") {
            getContactGroupsUseCase(contactId)
        }.getOrElse { emptyList() }

    /** Аккаунты для копирования и переноса: сбой не мешает смотреть контакт. */
    private suspend fun loadAccounts(): List<ContactAccount> =
        safeCall(DETAILS_TAG, "load accounts failed") {
            getContactAccountsUseCase()
        }.getOrElse { emptyList() }

    /** Копирует контакт в выбранный аккаунт: исходный остаётся на месте. */
    fun onCopyToAccount(account: ContactAccount) =
        changeAccount(account) { id, acc ->
            val newId = copyContactToAccountUseCase(id, acc)
            newId to DetailsEvent.ContactCopied(newId)
        }

    /** Переносит контакт в выбранный аккаунт: исходный удаляется. */
    fun onMoveToAccount(account: ContactAccount) =
        changeAccount(account) { id, acc ->
            val newId = moveContactToAccountUseCase(id, acc)
            newId to DetailsEvent.ContactMoved(newId)
        }

    /** Смена аккаунта: ошибка показывается тостом, контакт остаётся на месте. */
    private fun changeAccount(
        account: ContactAccount,
        action: suspend (Long, ContactAccount) -> Pair<Long, DetailsEvent>,
    ) {
        val contactId = selectedContactId.value ?: return
        viewModelScope.launch {
            safeCall(DETAILS_TAG, "change contact account failed") {
                val (newId, event) = action(contactId, account)
                eventFlow.value = event
                if (newId != contactId) {
                    loadedDetails.value = loadedDetails.value?.copy(contactId = newId)
                }
            }.onFailure { failure ->
                eventFlow.value = DetailsEvent.AccountActionFailed
            }
        }
    }

    /** Событие обработано экраном. */
    fun onEventHandled() {
        eventFlow.value = null
    }
}
