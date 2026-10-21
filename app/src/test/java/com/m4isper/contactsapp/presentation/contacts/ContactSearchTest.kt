package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.ContactRelation
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.Organization
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.PostalAddress
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.model.Website
import org.junit.Assert.assertEquals
import org.junit.Test

private fun search(
    contacts: List<ContactDetails>,
    query: String,
    selectedGroupId: Long? = null,
    membership: Map<Long, Set<Long>> = emptyMap(),
    groups: List<ContactGroup> = emptyList(),
    selectedAccount: ContactAccount? = null,
): List<String> =
    filterContacts(
        contacts = contacts,
        query = query,
        favoritesOnly = false,
        favorites = emptySet(),
        selectedGroupId = selectedGroupId,
        membership = membership,
        groups = groups,
        selectedAccount = selectedAccount,
    ).map { it.name }

class ContactSearchTest {
    @Test
    fun `phone search ignores formatting and matches partial digits`() {
        val contacts =
            listOf(
                ContactDetails(
                    contactId = 1,
                    name = "Anna",
                    phones = listOf(PhoneNumber("+7 (999) 123-45-67", PhoneKind.MOBILE)),
                ),
                ContactDetails(
                    contactId = 2,
                    name = "Bob",
                    phones = listOf(PhoneNumber("+1 555 000-11-22", PhoneKind.MOBILE)),
                ),
            )

        assertEquals(listOf("Anna"), search(contacts, "999123"))
        assertEquals(listOf("Anna"), search(contacts, "+7 999 123-45-67"))
        assertEquals(listOf("Anna"), search(contacts, "8 (999) 123-45-67"))
    }

    @Test
    fun `phone search matches last digits without country code`() {
        assertEquals(true, phoneDigitsMatch("79991234567", "9991234567"))
        assertEquals(true, phoneDigitsMatch("79991234567", "999123"))
        assertEquals(false, phoneDigitsMatch("79991234567", "999124"))
    }

    @Test
    fun `search covers company title address website and note`() {
        val contacts =
            listOf(
                ContactDetails(
                    contactId = 1,
                    name = "Anna",
                    organization = Organization(company = "ACME", title = "CTO"),
                    addresses = listOf(PostalAddress("Tverskaya 1", AddressKind.WORK)),
                    websites = listOf(Website("https://anna.dev")),
                    note = "Met at a conference",
                ),
                ContactDetails(contactId = 2, name = "Bob"),
            )

        listOf("acme", "cto", "tverskaya", "anna.dev", "conference").forEach { query ->
            assertEquals(query, listOf("Anna"), search(contacts, query))
        }
    }

    @Test
    fun `search covers group names`() {
        val contacts =
            listOf(
                ContactDetails(contactId = 1, name = "Anna"),
                ContactDetails(contactId = 2, name = "Bob"),
            )
        val groups = listOf(ContactGroup(id = 100, title = "Family"))
        val membership = mapOf(1L to setOf(100L))

        assertEquals(
            listOf("Anna"),
            search(
                contacts = contacts,
                query = "family",
                membership = membership,
                groups = groups,
            ),
        )
    }

    @Test
    fun `search covers custom labels`() {
        val contact =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                phones = listOf(PhoneNumber("111", PhoneKind.OTHER, customLabel = "Sister")),
                emails = listOf(EmailAddress("anna@example.com", EmailKind.OTHER, customLabel = "Side")),
                addresses = listOf(PostalAddress("Moscow", AddressKind.OTHER, customLabel = "Dacha")),
                relations =
                    listOf(
                        ContactRelation("Bob", RelationKind.FRIEND, customLabel = "Кум"),
                    ),
            )

        listOf("sister", "side", "dacha", "кум").forEach { query ->
            assertEquals(query, listOf("Anna"), search(listOf(contact), query))
        }
    }

    @Test
    fun `account filter limits the list`() {
        val google = ContactAccount(name = "user@gmail.com", type = "com.google")
        val contacts =
            listOf(
                ContactDetails(contactId = 1, name = "Anna", account = ContactAccount.Local),
                ContactDetails(contactId = 2, name = "Bob", account = google),
            )

        assertEquals(
            listOf("Anna"),
            search(contacts, query = "", selectedAccount = ContactAccount.Local),
        )
        assertEquals(listOf("Bob"), search(contacts, query = "", selectedAccount = google))
        assertEquals(listOf("Anna", "Bob"), search(contacts, query = ""))
    }
}
