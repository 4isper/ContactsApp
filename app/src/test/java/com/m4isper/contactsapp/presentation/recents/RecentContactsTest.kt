package com.m4isper.contactsapp.presentation.recents

import com.m4isper.contactsapp.domain.model.ContactDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun contact(
    id: Long,
    name: String,
    lastContacted: Long? = null,
    timesContacted: Int = 0,
) = ContactDetails(
    contactId = id,
    name = name,
    lastContacted = lastContacted,
    timesContacted = timesContacted,
)

class RecentContactsTest {
    @Test
    fun `keeps only contacted contacts sorted newest first`() {
        val contacts =
            listOf(
                contact(1, "Anna", lastContacted = 100, timesContacted = 1),
                contact(2, "Bob"),
                contact(3, "Cara", lastContacted = 300, timesContacted = 5),
                contact(4, "Dana", lastContacted = 200, timesContacted = 2),
            )

        val recents = recentContacts(contacts)

        assertEquals(listOf(3L, 4L, 1L), recents.map { it.contactId })
    }

    @Test
    fun `no contacted contacts returns empty list`() {
        assertTrue(recentContacts(listOf(contact(1, "Anna"), contact(2, "Bob"))).isEmpty())
    }

    @Test
    fun `same timestamp is ordered by name`() {
        val contacts =
            listOf(
                contact(1, "Cara", lastContacted = 100),
                contact(2, "Anna", lastContacted = 100),
            )

        assertEquals(listOf(2L, 1L), recentContacts(contacts).map { it.contactId })
    }
}
