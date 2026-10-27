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

class ContactGroupingFullTest {
    @Test
    fun contactSortKey_allOrders() {
        val c = contact(1, "Anna Petrova", family = "Petrova", company = "Acme")
        assertEquals("Anna Petrova", contactSortKey(c, ContactSortOrder.BY_NAME))
        assertEquals("Anna Petrova", contactSortKey(c, ContactSortOrder.BY_NAME_DESC))
        assertEquals("Anna Petrova", contactSortKey(c, ContactSortOrder.RECENTLY_ADDED))
        assertEquals("Petrova", contactSortKey(c, ContactSortOrder.BY_FAMILY))
        assertEquals("Acme", contactSortKey(c, ContactSortOrder.BY_COMPANY))
    }

    @Test
    fun contactSortKey_fallbackToName() {
        val c = contact(1, "JustName")
        assertEquals("JustName", contactSortKey(c, ContactSortOrder.BY_FAMILY))
        assertEquals("JustName", contactSortKey(c, ContactSortOrder.BY_COMPANY))
    }

    @Test
    fun sortContacts_byNameDesc() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Zoe"))
        val sorted = sortContacts(contacts, ContactSortOrder.BY_NAME_DESC)
        assertEquals(listOf(2L, 1L), sorted.map { it.contactId })
    }

    @Test
    fun sortContacts_recentlyAdded() {
        val contacts =
            listOf(
                contact(1, "Old", lastUpdated = 100L),
                contact(2, "New", lastUpdated = 999L),
            )
        val sorted = sortContacts(contacts, ContactSortOrder.RECENTLY_ADDED)
        assertEquals(listOf(2L, 1L), sorted.map { it.contactId })
    }

    @Test
    fun groupContactsByFirstLetter_recentlyAddedIsFlat() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val grouped = groupContactsByFirstLetter(contacts, ContactSortOrder.RECENTLY_ADDED)
        assertEquals(1, grouped.size)
        assertTrue(grouped.containsKey('#'))
    }

    @Test
    fun groupContactsByFirstLetter_nameDescSorted() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"), contact(3, "Zoe"))
        val grouped = groupContactsByFirstLetter(contacts, ContactSortOrder.BY_NAME_DESC)
        assertEquals(listOf('Z', 'B', 'A'), grouped.keys.toList())
    }

    @Test
    fun groupContactsWithPinnedFavorites_pinned() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val grouped = groupContactsWithPinnedFavorites(contacts, setOf(1L), ContactSortOrder.BY_NAME, true)
        assertEquals(listOf('★', 'B'), grouped.keys.toList())
        assertEquals(listOf(1L), grouped.getValue('★').map { it.contactId })
    }

    @Test
    fun groupContactsWithPinnedFavorites_notPinned() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val grouped = groupContactsWithPinnedFavorites(contacts, setOf(1L), ContactSortOrder.BY_NAME, false)
        assertEquals(listOf('A', 'B'), grouped.keys.toList())
    }

    @Test
    fun letterIndices_empty() {
        assertTrue(letterIndices(emptyMap()).isEmpty())
    }

    @Test
    fun letterIndices_calculatesOffsets() {
        val grouped =
            mapOf(
                'A' to listOf(contact(1, "Anna")),
                'B' to listOf(contact(2, "Bob"), contact(3, "Bill")),
            )
        val indices = letterIndices(grouped)
        assertEquals(0, indices['A'])
        assertEquals(2, indices['B'])
    }
}
