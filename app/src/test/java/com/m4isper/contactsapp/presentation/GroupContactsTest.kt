package com.m4isper.contactsapp.presentation

import com.m4isper.contactsapp.domain.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupContactsTest {

    @Test
    fun `groups by uppercase first letter sorted`() {
        val contacts = listOf(
            Contact(id = 1, name = "bob", phone = "1"),
            Contact(id = 2, name = "Anna", phone = "2"),
            Contact(id = 3, name = "alex", phone = "3"),
        )

        val grouped = groupContactsByFirstLetter(contacts)

        assertEquals(listOf('A', 'B'), grouped.keys.toList())
        assertEquals(2, grouped['A']?.size)
        assertEquals(1, grouped['B']?.size)
    }

    @Test
    fun `non-letter first char goes to hash group`() {
        val contacts = listOf(
            Contact(id = 1, name = "1st Contact", phone = "1"),
            Contact(id = 2, name = "+Unknown", phone = "2"),
        )

        val grouped = groupContactsByFirstLetter(contacts)

        assertEquals(setOf('#'), grouped.keys)
        assertEquals(2, grouped['#']?.size)
    }

    @Test
    fun `blank names are filtered out`() {
        val contacts = listOf(
            Contact(id = 1, name = "   ", phone = "1"),
            Contact(id = 2, name = "Anna", phone = "2"),
        )

        val grouped = groupContactsByFirstLetter(contacts)

        assertEquals(setOf('A'), grouped.keys)
    }

    @Test
    fun `empty list returns empty map`() {
        assertTrue(groupContactsByFirstLetter(emptyList()).isEmpty())
    }
}
