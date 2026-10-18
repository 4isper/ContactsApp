package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.NameParts
import java.util.Locale

/** Фильтр списка: группа, избранное и поиск по имени (включая фонетику), телефону, email, никнейму и связям. */
internal fun filterContacts(
    contacts: List<ContactDetails>,
    query: String,
    favoritesOnly: Boolean,
    favorites: Set<Long>,
    selectedGroupId: Long? = null,
    membership: Map<Long, Set<Long>> = emptyMap(),
): List<ContactDetails> {
    val byGroup =
        if (selectedGroupId == null) {
            contacts
        } else {
            contacts.filter { selectedGroupId in membership[it.contactId].orEmpty() }
        }
    val base = if (favoritesOnly) byGroup.filter { it.contactId in favorites } else byGroup
    val trimmed = query.trim()
    if (trimmed.isBlank()) return base
    val q = trimmed.lowercase(Locale.ROOT)
    return base.filter { contact ->
        contact.name.lowercase(Locale.ROOT).contains(q) ||
            contact.phones.any { it.number.contains(trimmed) } ||
            contact.emails.any { it.address.lowercase(Locale.ROOT).contains(q) } ||
            contact.nickname?.lowercase(Locale.ROOT)?.contains(q) == true ||
            contact.relations.any { it.name.lowercase(Locale.ROOT).contains(q) } ||
            contact.nameParts?.matches(q) == true
    }
}

/** Совпадение поискового запроса с любым компонентом структурированного имени. */
private fun NameParts.matches(query: String): Boolean =
    listOf(given, family, middle, prefix, suffix, phoneticGiven, phoneticMiddle, phoneticFamily)
        .any { it.lowercase(Locale.ROOT).contains(query) }
