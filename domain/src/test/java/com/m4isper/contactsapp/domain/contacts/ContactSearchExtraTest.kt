package com.m4isper.contactsapp.domain.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactSearchExtraTest {
    private fun contact(
        id: Long,
        name: String,
        account: ContactAccount = ContactAccount.Local,
    ) = ContactDetails(contactId = id, name = name, account = account)

    @Test
    fun filterContacts_filtersByGroup() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val result =
            filterContacts(
                contacts = contacts,
                query = "",
                selectedGroupId = 10L,
                membership = mapOf(1L to setOf(10L)),
            )
        assertEquals(listOf(1L), result.map { it.contactId })
    }

    @Test
    fun filterContacts_filtersByAccount() {
        val contacts =
            listOf(
                contact(1, "Anna", ContactAccount()),
                contact(2, "Bob", ContactAccount("a", "b")),
            )
        val result = filterContacts(contacts, query = "", selectedAccount = ContactAccount())
        assertEquals(listOf(1L), result.map { it.contactId })
    }

    @Test
    fun filterContacts_favoritesOnly() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val result = filterContacts(contacts, query = "", favoritesOnly = true, favorites = setOf(2L))
        assertEquals(listOf(2L), result.map { it.contactId })
    }

    @Test
    fun filterContacts_blankQueryReturnsBase() {
        val contacts = listOf(contact(1, "Anna"), contact(2, "Bob"))
        val result = filterContacts(contacts, query = "   ")
        assertEquals(2, result.size)
    }

    @Test
    fun phoneDigitsMatch_suffixAndSubstring() {
        assertTrue(phoneDigitsMatch("1234567890", "7890"))
        assertTrue(phoneDigitsMatch("1234567890", "345"))
    }
}
