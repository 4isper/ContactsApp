package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import java.util.Locale

/**
 * Ключ сортировки: полное имя или фамилия (с откатом к полному имени,
 * если фамилии в структурированном имени нет).
 */
internal fun contactSortKey(
    contact: ContactDetails,
    sortOrder: ContactSortOrder,
): String =
    when (sortOrder) {
        ContactSortOrder.BY_NAME -> contact.name
        ContactSortOrder.BY_FAMILY ->
            contact.nameParts
                ?.family
                ?.takeIf { it.isNotBlank() }
                ?: contact.name
    }

/** Контакты без имени отбрасываются; внутри буквы порядок — по ключу сортировки. */
internal fun groupContactsByFirstLetter(
    contacts: List<ContactDetails>,
    sortOrder: ContactSortOrder = ContactSortOrder.BY_NAME,
): Map<Char, List<ContactDetails>> =
    contacts
        .filter { it.name.isNotBlank() }
        .sortedBy { contactSortKey(it, sortOrder).lowercase(Locale.ROOT) }
        .groupBy {
            val first = contactSortKey(it, sortOrder).trim().first().uppercaseChar()
            if (first.isLetter()) first else '#'
        }.toSortedMap()

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
