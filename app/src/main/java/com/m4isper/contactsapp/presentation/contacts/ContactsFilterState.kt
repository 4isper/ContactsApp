package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactAccount
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Фильтры списка: поисковый запрос, режим «только избранное», группа и аккаунт.
 * Смена фильтра снимает выделение: выделение относится к видимым контактам.
 */
@Inject
internal class ContactsFilterState(
    private val selection: ContactsSelectionState,
) {
    private val _searchQuery = MutableStateFlow("")

    /** Поисковый запрос. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _favoritesOnly = MutableStateFlow(false)

    /** Показывать только избранное. */
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()
    private val _selectedGroupId = MutableStateFlow<Long?>(null)

    /** Выбранная группа; null — показать все контакты. */
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()
    private val _selectedAccount = MutableStateFlow<ContactAccount?>(null)

    /** Выбранный аккаунт; null — показать контакты всех аккаунтов. */
    val selectedAccount: StateFlow<ContactAccount?> = _selectedAccount.asStateFlow()

    fun onSearchQueryChange(query: String) {
        selection.clear()
        _searchQuery.value = query
    }

    /** Включает фильтр избранного (без переключения). */
    fun onShowFavoritesOnly() {
        if (!_favoritesOnly.value) _favoritesOnly.value = true
    }

    fun onToggleFavoritesOnly() {
        selection.clear()
        _favoritesOnly.value = !_favoritesOnly.value
    }

    /** Фильтр по группе; null — показать все контакты. */
    fun onGroupSelected(groupId: Long?) {
        selection.clear()
        _selectedGroupId.value = groupId
    }

    /** Фильтр по аккаунту; null — показать контакты всех аккаунтов. */
    fun onAccountSelected(account: ContactAccount?) {
        selection.clear()
        _selectedAccount.value = account
    }

    /** Группы больше нет: фильтр по ней снимается. */
    fun onGroupMissing() {
        _selectedGroupId.value = null
    }

    /** Аккаунта больше нет: фильтр по нему снимается. */
    fun onAccountMissing() {
        _selectedAccount.value = null
    }
}
