package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun details(id: Long, name: String, vararg phones: String) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
)

class GroupContactsTest {

    @Test
    fun `groups by uppercase first letter sorted`() {
        val contacts = listOf(
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
        val contacts = listOf(
            details(1, "1st Contact", "1"),
            details(2, "+Unknown", "2"),
        )

        val grouped = groupContactsByFirstLetter(contacts)

        assertEquals(setOf('#'), grouped.keys)
        assertEquals(2, grouped['#']?.size)
    }

    @Test
    fun `blank names are filtered out`() {
        val contacts = listOf(
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
}
