package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import java.util.Locale

private const val RECENTLY_ADDED_GROUP = '#'

/**
 * Ключ сортировки: полное имя, фамилия или компания. Для недавних контактов
 * ключ не важен, поэтому используется имя — как безопасный fallback.
 */
internal fun contactSortKey(
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
internal fun sortContacts(
    contacts: List<ContactDetails>,
    sortOrder: ContactSortOrder,
): List<ContactDetails> {
    val key: (ContactDetails) -> String = {
        contactSortKey(it, sortOrder).lowercase(Locale.ROOT)
    }
    val comparator =
        when (sortOrder) {
            ContactSortOrder.BY_NAME_DESC ->
                compareByDescending(key).thenByDescending { it.name.lowercase(Locale.ROOT) }
            ContactSortOrder.RECENTLY_ADDED ->
                compareByDescending<ContactDetails> { it.lastUpdated ?: Long.MIN_VALUE }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            else -> compareBy(key).thenBy { it.name.lowercase(Locale.ROOT) }
        }
    return contacts.sortedWith(comparator)
}

/**
 * Контакты без имени отбрасываются. Для обычных сортировок список группируется
 * по первой букве ключа, для «сначала новые» — остаётся плоским.
 */
internal fun groupContactsByFirstLetter(
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
                    val first = contactSortKey(it, sortOrder).trim().first().uppercaseChar()
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

/** Индекс первого элемента (sticky-заголовка) каждой буквы в LazyColumn. */
internal fun letterIndices(grouped: Map<Char, List<ContactDetails>>): Map<Char, Int> {
    var index = 0
    return buildMap {
        grouped.forEach { (letter, group) ->
            put(letter, index)
            index += 1 + group.size
        }
    }
}
