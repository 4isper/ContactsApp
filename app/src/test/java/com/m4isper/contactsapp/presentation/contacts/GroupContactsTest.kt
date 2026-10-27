package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.contacts.FAVORITES_GROUP
import com.m4isper.contactsapp.domain.contacts.groupContactsByFirstLetter
import com.m4isper.contactsapp.domain.contacts.groupContactsWithPinnedFavorites
import com.m4isper.contactsapp.domain.contacts.letterIndices
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.presentation.contacts.components.letterIndexAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun details(
    id: Long,
    name: String,
    vararg phones: String,
) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
)

class GroupContactsTest {
    @Test
    fun `groups by uppercase first letter sorted`() {
        val contacts =
            listOf(
                details(10, "bob", "1"),
                details(20, "Anna", "2"),
                details(30, "alex", "3"),
            )

        val grouped = groupContactsByFirstLetter(contacts)

        assertEquals(listOf('A', 'B'), grouped.keys.toList())
        assertEquals(2, grouped['A']?.size)
        assertEquals(1, grouped['B']?.size)
    }

    @Test
    fun `non-letter first char goes to hash group`() {
        val contacts =
            listOf(
                details(1, "1st Contact", "1"),
                details(2, "+Unknown", "2"),
            )

        val grouped = groupContactsByFirstLetter(contacts)

        assertEquals(setOf('#'), grouped.keys)
        assertEquals(2, grouped['#']?.size)
    }

    @Test
    fun `blank names are filtered out`() {
        val contacts =
            listOf(
                details(1, "   ", "1"),
                details(2, "Anna", "2"),
            )

        val grouped = groupContactsByFirstLetter(contacts)

        assertEquals(setOf('A'), grouped.keys)
    }

    @Test
    fun `empty list returns empty map`() {
        assertTrue(groupContactsByFirstLetter(emptyList()).isEmpty())
    }

    @Test
    fun `pinned favorites go first without duplicating alphabetical groups`() {
        val contacts =
            listOf(
                details(1, "Anna", "1"),
                details(2, "Bob", "2"),
                details(3, "Cara", "3"),
            )

        val grouped =
            groupContactsWithPinnedFavorites(
                contacts = contacts,
                favorites = setOf(3),
                sortOrder = ContactSortOrder.BY_NAME,
                pinFavorites = true,
            )

        assertEquals(listOf(FAVORITES_GROUP, 'A', 'B'), grouped.keys.toList())
        assertEquals(listOf(3L), grouped.getValue(FAVORITES_GROUP).map { it.contactId })
        assertEquals(listOf(1L), grouped.getValue('A').map { it.contactId })
        assertEquals(listOf(2L), grouped.getValue('B').map { it.contactId })
    }

    @Test
    fun `pinned section is skipped when disabled`() {
        val contacts = listOf(details(1, "Anna", "1"), details(2, "Bob", "2"))

        val grouped =
            groupContactsWithPinnedFavorites(
                contacts = contacts,
                favorites = setOf(2),
                sortOrder = ContactSortOrder.BY_NAME,
                pinFavorites = false,
            )

        assertEquals(listOf('A', 'B'), grouped.keys.toList())
    }

    @Test
    fun `pinned favorites keep recently added section flat`() {
        val contacts =
            listOf(
                ContactDetails(contactId = 1, name = "Anna", lastUpdated = 100),
                ContactDetails(contactId = 2, name = "Bob", lastUpdated = 300),
            )

        val grouped =
            groupContactsWithPinnedFavorites(
                contacts = contacts,
                favorites = setOf(1),
                sortOrder = ContactSortOrder.RECENTLY_ADDED,
                pinFavorites = true,
            )

        assertEquals(listOf(FAVORITES_GROUP, '#'), grouped.keys.toList())
        assertEquals(listOf(2L), grouped.getValue('#').map { it.contactId })
    }

    @Test
    fun `letter indices point at sticky headers`() {
        val grouped =
            groupContactsByFirstLetter(
                listOf(
                    details(1, "bob", "1"),
                    details(2, "Anna", "2"),
                    details(3, "alex", "3", "4"),
                ),
            )

        val indices = letterIndices(grouped)

        // A header at 0, two Annas at 1-2, B header at 3, bob at 4.
        assertEquals(mapOf('A' to 0, 'B' to 3), indices)
    }

    @Test
    fun `letter indices of empty map is empty`() {
        assertTrue(letterIndices(emptyMap()).isEmpty())
    }

    @Test
    fun `letter index at maps touch position to index`() {
        // 4 буквы на высоте 1000px: каждая занимает 250px.
        assertEquals(0, letterIndexAt(y = 0f, height = 1000, size = 4))
        assertEquals(0, letterIndexAt(y = 249f, height = 1000, size = 4))
        assertEquals(1, letterIndexAt(y = 250f, height = 1000, size = 4))
        assertEquals(3, letterIndexAt(y = 999f, height = 1000, size = 4))
    }

    @Test
    fun `letter index at clamps out of bounds touches`() {
        assertEquals(0, letterIndexAt(y = -50f, height = 1000, size = 4))
        assertEquals(3, letterIndexAt(y = 5000f, height = 1000, size = 4))
    }

    @Test
    fun `letter index at returns null for degenerate input`() {
        assertEquals(null, letterIndexAt(y = 10f, height = 0, size = 4))
        assertEquals(null, letterIndexAt(y = 10f, height = 1000, size = 0))
    }
}
