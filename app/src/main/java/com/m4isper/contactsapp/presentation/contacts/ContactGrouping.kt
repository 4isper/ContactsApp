package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails

internal fun groupContactsByFirstLetter(contacts: List<ContactDetails>): Map<Char, List<ContactDetails>> =
    contacts
        .filter { it.name.isNotBlank() }
        .groupBy {
            val first = it.name.first().uppercaseChar()
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
