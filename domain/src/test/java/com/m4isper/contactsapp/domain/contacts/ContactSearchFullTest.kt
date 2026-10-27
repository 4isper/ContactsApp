package com.m4isper.contactsapp.domain.contacts

import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.PostalAddress
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactSearchFullTest {
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
                contact(1, "Anna", ContactAccount.Local),
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

    @Test
    fun contactDetailsPrimaryPhone() {
        val details =
            ContactDetails(
                contactId = 1L,
                name = "Anna",
                phones =
                    listOf(
                        com.m4isper.contactsapp.domain.model.contact.PhoneNumber(
                            "111",
                            com.m4isper.contactsapp.domain.model.contact.PhoneKind.MOBILE,
                        ),
                    ),
            )
        assertEquals("111", details.primaryPhone?.number)
    }

    @Test
    fun contactDetailsPrimaryEmail() {
        val details =
            ContactDetails(
                contactId = 1L,
                name = "Anna",
                emails =
                    listOf(
                        com.m4isper.contactsapp.domain.model.contact.EmailAddress(
                            "work@example.com",
                            com.m4isper.contactsapp.domain.model.contact.EmailKind.WORK,
                        ),
                    ),
            )
        assertEquals("work@example.com", details.primaryEmail?.address)
    }

    @Test
    fun postalAddressDisplayAddress() {
        val addr =
            PostalAddress(
                address = "fallback",
                kind = AddressKind.HOME,
                street = "Lenina 1",
                city = "Moscow",
                country = "Russia",
            )
        assertTrue(addr.displayAddress.isNotEmpty())
        assertTrue(addr.streetLine.isNotEmpty())
    }

    @Test
    fun relationKindValues() {
        assertTrue(RelationKind.values().isNotEmpty())
        assertTrue(RelationKind.OTHER.toString().isNotEmpty())
    }
}
