package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.NameParts
import org.junit.Assert.assertEquals
import org.junit.Test

private fun named(
    id: Long,
    name: String,
    family: String = "",
) = ContactDetails(
    contactId = id,
    name = name,
    nameParts = if (family.isBlank()) null else NameParts(family = family),
)

class ContactSortingTest {
    @Test
    fun `family sort groups by family initial and orders members`() {
        val contacts =
            listOf(
                named(1, "Anna Petrova", family = "Petrova"),
                named(2, "Bob Ivanov", family = "Ivanov"),
                named(3, "Anna Ivanova", family = "Ivanova"),
            )

        val grouped = groupContactsByFirstLetter(contacts, ContactSortOrder.BY_FAMILY)

        assertEquals(listOf('I', 'P'), grouped.keys.toList())
        assertEquals(listOf(2L, 3L), grouped.getValue('I').map { it.contactId })
        assertEquals(listOf(1L), grouped.getValue('P').map { it.contactId })
    }

    @Test
    fun `family sort falls back to full name without structured family`() {
        val contacts =
            listOf(
                named(1, "Zoya"),
                named(2, "Bob Ivanov", family = "Ivanov"),
            )

        val grouped = groupContactsByFirstLetter(contacts, ContactSortOrder.BY_FAMILY)

        assertEquals(listOf('I', 'Z'), grouped.keys.toList())
        assertEquals(listOf(1L), grouped.getValue('Z').map { it.contactId })
    }

    @Test
    fun `name sort orders groups case insensitively`() {
        val contacts =
            listOf(
                named(1, "bob"),
                named(2, "Anna"),
                named(3, "alex"),
            )

        val grouped = groupContactsByFirstLetter(contacts, ContactSortOrder.BY_NAME)

        assertEquals(listOf(3L, 2L), grouped.getValue('A').map { it.contactId })
    }

    @Test
    fun `sort key uses family for family order and full name otherwise`() {
        assertEquals("Anna", contactSortKey(named(1, "Anna"), ContactSortOrder.BY_FAMILY))
        assertEquals(
            "Ivanov",
            contactSortKey(named(2, "Bob Ivanov", family = "Ivanov"), ContactSortOrder.BY_FAMILY),
        )
        assertEquals(
            "Bob Ivanov",
            contactSortKey(named(2, "Bob Ivanov", family = "Ivanov"), ContactSortOrder.BY_NAME),
        )
    }
}
