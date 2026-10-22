// Подавление осознанное: файл разбит на маленькие предикаты поиска по разным полям.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.domain.contacts

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.normalizeContactText
import com.m4isper.contactsapp.domain.model.normalizePhoneDigits

private const val MIN_PHONE_SUFFIX_DIGITS = 7
private const val MAX_PHONE_SUFFIX_DIGITS = 10

/**
 * Фильтр списка: группа, аккаунт, избранное и поиск по всем доступным полям.
 * Телефоны сравниваются по цифрам, чтобы форматирование и код страны не мешали.
 */
fun filterContacts(
    contacts: List<ContactDetails>,
    query: String,
    favoritesOnly: Boolean = false,
    favorites: Set<Long> = emptySet(),
    selectedGroupId: Long? = null,
    membership: Map<Long, Set<Long>> = emptyMap(),
    groups: List<ContactGroup> = emptyList(),
    selectedAccount: ContactAccount? = null,
): List<ContactDetails> {
    val byGroup =
        if (selectedGroupId == null) {
            contacts
        } else {
            contacts.filter { selectedGroupId in membership[it.contactId].orEmpty() }
        }
    val byAccount =
        if (selectedAccount == null) {
            byGroup
        } else {
            byGroup.filter { it.account == selectedAccount }
        }
    val base = if (favoritesOnly) byAccount.filter { it.contactId in favorites } else byAccount
    val trimmed = query.trim()
    if (trimmed.isBlank()) return base
    val normalizedQuery = normalizeContactText(trimmed)
    val queryDigits = normalizePhoneDigits(trimmed)
    val groupTitleById = groups.associate { it.id to it.title }
    return base.filter { contact ->
        contact.matches(
            query = normalizedQuery,
            queryDigits = queryDigits,
            groupTitleById = groupTitleById,
            membership = membership,
        )
    }
}

private fun ContactDetails.matches(
    query: String,
    queryDigits: String,
    groupTitleById: Map<Long, String>,
    membership: Map<Long, Set<Long>>,
): Boolean =
    name.containsText(query) ||
        nameParts?.matches(query) == true ||
        matchesPhone(query, queryDigits) ||
        matchesContactFields(query) ||
        matchesRelationsAndMessages(query) ||
        matchesGroup(query, groupTitleById, membership)

private fun ContactDetails.matchesPhone(
    query: String,
    queryDigits: String,
): Boolean = phones.any { it.matches(query, queryDigits) }

private fun ContactDetails.matchesContactFields(query: String): Boolean =
    matchesEmails(query) ||
        matchesAddresses(query) ||
        matchesWebsites(query) ||
        matchesOrganization(query) ||
        note.containsText(query) ||
        nickname.containsText(query)

private fun ContactDetails.matchesEmails(query: String): Boolean =
    emails.any { it.address.containsText(query) || it.customLabel.containsText(query) }

private fun ContactDetails.matchesAddresses(query: String): Boolean =
    addresses.any { it.displayAddress.containsText(query) || it.customLabel.containsText(query) }

private fun ContactDetails.matchesWebsites(query: String): Boolean =
    websites.any { it.url.containsText(query) || it.customLabel.containsText(query) }

private fun ContactDetails.matchesOrganization(query: String): Boolean =
    organization?.let {
        it.company.containsText(query) ||
            it.title.containsText(query) ||
            it.department.containsText(query) ||
            it.jobDescription.containsText(query) ||
            it.officeLocation.containsText(query)
    } == true

private fun ContactDetails.matchesRelationsAndMessages(query: String): Boolean =
    relations.any { it.name.containsText(query) || it.customLabel.containsText(query) } ||
        events.any { it.customLabel.containsText(query) } ||
        instantMessages.any { it.address.containsText(query) || it.customProtocol.containsText(query) } ||
        sipAddresses.any { it.address.containsText(query) || it.customLabel.containsText(query) }

private fun ContactDetails.matchesGroup(
    query: String,
    groupTitleById: Map<Long, String>,
    membership: Map<Long, Set<Long>>,
): Boolean =
    membership[contactId].orEmpty().any { groupId ->
        groupTitleById[groupId].containsText(query)
    }

private fun PhoneNumber.matches(
    query: String,
    queryDigits: String,
): Boolean =
    number.containsText(query) ||
        (queryDigits.isNotEmpty() && phoneDigitsMatch(normalizePhoneDigits(number), queryDigits)) ||
        customLabel.containsText(query)

/** Совпадение цифр номера: подстрока или последние 7–10 цифр с учётом кода страны. */
fun phoneDigitsMatch(
    numberDigits: String,
    queryDigits: String,
): Boolean {
    if (queryDigits.isEmpty() || numberDigits.isEmpty()) return false
    val compared = minOf(queryDigits.length, MAX_PHONE_SUFFIX_DIGITS)
    val suffixMatches =
        queryDigits.length >= MIN_PHONE_SUFFIX_DIGITS &&
            numberDigits.length >= compared &&
            numberDigits.takeLast(compared) == queryDigits.takeLast(compared)
    return numberDigits.contains(queryDigits) || suffixMatches
}

private fun String?.containsText(query: String): Boolean = this?.let(::normalizeContactText)?.contains(query) == true

/** Совпадение поискового запроса с любым компонентом структурированного имени. */
private fun NameParts.matches(query: String): Boolean =
    listOf(given, family, middle, prefix, suffix, phoneticGiven, phoneticMiddle, phoneticFamily)
        .any { normalizeContactText(it).contains(query) }
