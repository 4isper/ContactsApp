package com.m4isper.contactsapp.presentation.contacts

/**
 * Причина, по которой список контактов пуст. От неё зависят заголовок и действия:
 * при активном фильтре нельзя предлагать создать контакт, пока пользователь не увидит
 * способ вернуться ко всем контактам.
 */
internal sealed interface ContactsEmptyReason {
    /** В адресной книге действительно нет контактов. */
    data object NoContacts : ContactsEmptyReason

    /** Ничего не найдено по поисковому запросу. */
    data class Search(
        val query: String,
        val hasOtherFilters: Boolean,
    ) : ContactsEmptyReason

    /** Включён фильтр «только избранное», но избранных контактов нет. */
    data object Favorites : ContactsEmptyReason

    /** В выбранной группе нет контактов. */
    data class Group(
        val groupName: String,
    ) : ContactsEmptyReason

    /** В выбранном аккаунте нет контактов. */
    data class Account(
        val accountName: String,
    ) : ContactsEmptyReason

    /** Одновременно активны несколько фильтров, и ни один контакт им не соответствует. */
    data object CombinedFilters : ContactsEmptyReason
}

/**
 * Определяет причину пустого списка. Имена группы и аккаунта передаются уже
 * локализованными: слой состояния не знает о ресурсах.
 *
 * Поиск имеет приоритет: если он активен, пользователь прежде всего должен увидеть,
 * что именно запрос скрывает контакты. Комбинация двух и более фильтров без поиска даёт
 * [ContactsEmptyReason.CombinedFilters].
 */
internal fun contactsEmptyReason(
    searchQuery: String,
    showFavoritesOnly: Boolean,
    groupName: String?,
    accountName: String?,
): ContactsEmptyReason {
    val activeFilters =
        listOfNotNull(
            if (showFavoritesOnly) FILTER_FAVORITES else null,
            if (groupName != null) FILTER_GROUP else null,
            if (accountName != null) FILTER_ACCOUNT else null,
        )
    return when {
        searchQuery.isNotBlank() ->
            ContactsEmptyReason.Search(
                query = searchQuery,
                hasOtherFilters = activeFilters.isNotEmpty(),
            )

        activeFilters.size > 1 -> ContactsEmptyReason.CombinedFilters
        groupName != null -> ContactsEmptyReason.Group(groupName)
        accountName != null -> ContactsEmptyReason.Account(accountName)
        showFavoritesOnly -> ContactsEmptyReason.Favorites
        else -> ContactsEmptyReason.NoContacts
    }
}

private const val FILTER_FAVORITES = "favorites"

private const val FILTER_GROUP = "group"

private const val FILTER_ACCOUNT = "account"
