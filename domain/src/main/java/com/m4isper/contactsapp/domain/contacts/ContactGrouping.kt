package com.m4isper.contactsapp.domain.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.normalizeContactText

private const val RECENTLY_ADDED_GROUP = '#'

/** Синтетический ключ закреплённой секции избранного; не буква, поэтому не попадает в fast scroller. */
const val FAVORITES_GROUP = '★'

/**
 * Ключ сортировки: полное имя, фамилия или компания. Для недавних контактов
 * ключ не важен, поэтому используется имя — как безопасный fallback.
 */
fun contactSortKey(
    contact: ContactDetails,
    sortOrder: ContactSortOrder,
): String =
    when (sortOrder) {
        ContactSortOrder.BY_NAME,
        ContactSortOrder.BY_NAME_DESC,
        ContactSortOrder.RECENTLY_ADDED,
        -> contact.name
        ContactSortOrder.BY_FAMILY ->
            contact.nameParts
                ?.family
                ?.takeIf { it.isNotBlank() }
                ?: contact.name
        ContactSortOrder.BY_COMPANY ->
            contact.organization
                ?.company
                ?.takeIf { it.isNotBlank() }
                ?: contact.name
    }

/** Сортировка контактов с учётом направления; одинаковые ключи упорядочены по имени. */
fun sortContacts(
    contacts: List<ContactDetails>,
    sortOrder: ContactSortOrder,
): List<ContactDetails> {
    val key: (ContactDetails) -> String = {
        normalizeContactText(contactSortKey(it, sortOrder))
    }
    val comparator =
        when (sortOrder) {
            ContactSortOrder.BY_NAME_DESC ->
                compareByDescending(key).thenByDescending { normalizeContactText(it.name) }
            ContactSortOrder.RECENTLY_ADDED ->
                compareByDescending<ContactDetails> { it.lastUpdated ?: Long.MIN_VALUE }
                    .thenBy { normalizeContactText(it.name) }
            else -> compareBy(key).thenBy { normalizeContactText(it.name) }
        }
    return contacts.sortedWith(comparator)
}

/**
 * Контакты без имени отбрасываются. Для обычных сортировок список группируется
 * по первой букве ключа, для «сначала новые» — остаётся плоским.
 */
fun groupContactsByFirstLetter(
    contacts: List<ContactDetails>,
    sortOrder: ContactSortOrder = ContactSortOrder.BY_NAME,
): Map<Char, List<ContactDetails>> {
    val sorted = sortContacts(contacts.filter { it.name.isNotBlank() }, sortOrder)
    return when {
        sorted.isEmpty() -> emptyMap()
        sortOrder == ContactSortOrder.RECENTLY_ADDED -> mapOf(RECENTLY_ADDED_GROUP to sorted)
        else ->
            sorted
                .groupBy {
                    val first = normalizeContactText(contactSortKey(it, sortOrder)).first().uppercaseChar()
                    if (first.isLetter()) first else '#'
                }.let { grouped ->
                    if (sortOrder == ContactSortOrder.BY_NAME_DESC) {
                        grouped.toSortedMap(compareByDescending { it })
                    } else {
                        grouped.toSortedMap()
                    }
                }
    }
}

/**
 * Группирует список, при необходимости вынося избранные контакты в отдельную
 * секцию сверху. Остальные контакты сохраняют обычную буквенную группировку.
 */
fun groupContactsWithPinnedFavorites(
    contacts: List<ContactDetails>,
    favorites: Set<Long>,
    sortOrder: ContactSortOrder,
    pinFavorites: Boolean,
): Map<Char, List<ContactDetails>> {
    if (!pinFavorites) return groupContactsByFirstLetter(contacts, sortOrder)
    val named = contacts.filter { it.name.isNotBlank() }
    val favoriteContacts = named.filter { it.contactId in favorites }
    val otherContacts = named.filterNot { it.contactId in favorites }
    val grouped = linkedMapOf<Char, List<ContactDetails>>()
    if (favoriteContacts.isNotEmpty()) {
        grouped[FAVORITES_GROUP] = sortContacts(favoriteContacts, sortOrder)
    }
    grouped.putAll(groupContactsByFirstLetter(otherContacts, sortOrder))
    return grouped
}

/** Индекс первого элемента (sticky-заголовка) каждой буквы в LazyColumn. */
fun letterIndices(grouped: Map<Char, List<ContactDetails>>): Map<Char, Int> {
    var index = 0
    return buildMap {
        grouped.forEach { (letter, group) ->
            put(letter, index)
            index += 1 + group.size
        }
    }
}
