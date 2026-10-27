package com.m4isper.contactsapp.domain.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.NameParts
import com.m4isper.contactsapp.domain.model.contact.Organization
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun contact(
    id: Long,
    name: String,
    family: String = "",
    company: String = "",
    lastUpdated: Long? = null,
) = ContactDetails(
    contactId = id,
    name = name,
    nameParts = if (family.isBlank()) null else NameParts(family = family),
    organization = if (company.isBlank()) null else Organization(company = company, title = ""),
    lastUpdated = lastUpdated,
)

class ContactGroupingExtraTest {
    @Test
    fun groupContactsWithPinnedFavorites_createsFavoritesSection() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val grouped = groupContactsWithPinnedFavorites(contacts, setOf(1L), ContactSortOrder.BY_NAME, true)
        assertEquals(listOf('★', 'B'), grouped.keys.toList())
        assertEquals(listOf(1L), grouped.getValue('★').map { it.contactId })
    }

    @Test
    fun groupContactsWithPinnedFavorites_withoutPinUsesNormalGrouping() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val grouped = groupContactsWithPinnedFavorites(contacts, setOf(1L), ContactSortOrder.BY_NAME, false)
        assertEquals(listOf('A', 'B'), grouped.keys.toList())
    }

    @Test
    fun letterIndices_mapsStartingPositions() {
        val grouped = mapOf('A' to listOf(contact(1, "Anna")), 'B' to listOf(contact(2, "Bob"), contact(3, "Bill")))
        val indices = letterIndices(grouped)
        assertEquals(0, indices['A'])
        assertEquals(2, indices['B'])
    }

    @Test
    fun groupContactsByFirstLetter_emptyReturnsEmptyMap() {
        assertTrue(groupContactsByFirstLetter(emptyList()).isEmpty())
    }

    @Test
    fun sortContacts_descendingByName() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Zoe"))
        val sorted = sortContacts(contacts, ContactSortOrder.BY_NAME_DESC)
        assertEquals(listOf(2L, 1L), sorted.map { it.contactId })
    }
}
