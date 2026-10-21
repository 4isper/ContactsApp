package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactDuplicatesTest {
    private val anna =
        ContactDetails(
            contactId = 1,
            name = "Anna",
            phones = listOf(PhoneNumber("+79161234567", PhoneKind.MOBILE)),
            emails = listOf(EmailAddress("anna@example.com", EmailKind.HOME)),
        )

    @Test
    fun `same phone means similar contact`() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "+7 916 123 45 67")))

        assertEquals(listOf(anna), findSimilarContacts(form, listOf(anna)))
    }

    @Test
    fun `same email means similar contact`() {
        val form = ContactForm(emails = listOf(EditableEmail(address = "ANNA@example.com")))

        assertEquals(listOf(anna), findSimilarContacts(form, listOf(anna)))
    }

    @Test
    fun `different contact is not similar`() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "+79001112233")))

        assertTrue(findSimilarContacts(form, listOf(anna)).isEmpty())
    }

    @Test
    fun `short phone is not a match`() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "123")))

        assertTrue(findSimilarContacts(form, listOf(anna)).isEmpty())
    }
}
