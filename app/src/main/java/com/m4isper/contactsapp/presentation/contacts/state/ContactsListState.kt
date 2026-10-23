package com.m4isper.contactsapp.presentation.contacts.state

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsChangeSource
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsPageUseCase
import com.m4isper.contactsapp.presentation.common.safeCall
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LIST_TAG = "ContactsList"

/** Размер страницы контактов: столько грузим за один запрос к провайдеру. */
private const val CONTACTS_PAGE_SIZE = 200

/**
 * Загрузка списка контактов: страницы, состояние загрузки и ошибка.
 * Список приходит постранично, чтобы список появлялся сразу.
 */
@Inject
internal class ContactsListState(
    private val getContactsPage: GetContactsPageUseCase,
    private val changesSource: ContactsChangeSource,
) {
    private val _contacts = MutableStateFlow<List<ContactDetails>>(emptyList())

    /** Загруженные контакты. */
    val contacts: StateFlow<List<ContactDetails>> = _contacts.asStateFlow()
    private val _isLoading = MutableStateFlow(false)

    /** Идёт ли загрузка. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _failure = MutableStateFlow<Throwable?>(null)
    private var pendingReload = false

    /** Ошибка последней загрузки. */
    val failure: StateFlow<Throwable?> = _failure.asStateFlow()

    /** Поток изменений провайдера: экран подписывается, пока список видим. */
    val providerChanges: Flow<Unit> = changesSource.observeChanges()

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.

    /**
     * Загружает список страницами: первая страница показывается сразу,
     * остальные добавляются следом. Если список уже грузится, следующая попытка
     * запоминается и выполняется после текущей: изменения провайдера не теряются.
     */
    suspend fun load() {
        if (_isLoading.value) {
            pendingReload = true
            return
        }
        _failure.value = null
        _isLoading.value = true
        try {
            safeCall(LIST_TAG, "refresh contacts failed") {
                loadByPages()
            }.onFailure { failure ->
                _failure.value = failure
            }
        } finally {
            _isLoading.value = false
        }
        if (pendingReload) {
            pendingReload = false
            load()
        }
    }

    /** Сбрасывает список: экран очищен, следующая загрузка начнётся с нуля. */
    fun clear() {
        _contacts.value = emptyList()
        _failure.value = null
    }

    /** Убирает удалённые контакты из локального списка, не перечитывая провайдера. */
    fun remove(contactIds: Set<Long>) {
        _contacts.value = _contacts.value.filterNot { it.contactId in contactIds }
    }

    private suspend fun loadByPages() {
        var offset = 0
        var first = true
        var hasMore = true
        while (hasMore) {
            val page = getContactsPage(offset, CONTACTS_PAGE_SIZE)
            _contacts.value = if (first) page else _contacts.value + page
            first = false
            offset += page.size
            hasMore = page.size == CONTACTS_PAGE_SIZE
        }
        if (first) _contacts.value = emptyList()
    }
}
