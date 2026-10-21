package com.m4isper.contactsapp.presentation.common.share

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.Organization
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactTextTest {
    @Test
    fun `text contains name phones and emails`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                phones = listOf(PhoneNumber(number = "+1 899", kind = PhoneKind.MOBILE)),
                emails = listOf(EmailAddress(address = "anna@example.com", kind = EmailKind.HOME)),
            )

        val text = contactAsText(details)

        assertEquals(listOf("Anna", "+1 899", "anna@example.com"), text.lines())
    }

    @Test
    fun `text contains the company when it is filled`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                organization = Organization(company = "Acme", title = "Engineer"),
            )

        val text = contactAsText(details)

        assertEquals(listOf("Anna", "Acme"), text.lines())
    }

    @Test
    fun `empty company is skipped`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                organization = Organization(company = "  ", title = "Engineer"),
            )

        val text = contactAsText(details)

        assertEquals(listOf("Anna"), text.lines())
    }

    @Test
    fun `qr matrix is generated for the vcard text`() {
        val matrix = qrMatrix("BEGIN:VCARD\\nFN:Anna\\nEND:VCARD", sizePx = 128)

        assertTrue(matrix != null)
        assertEquals(128 * 128, matrix!!.size)
        assertTrue(matrix.any { it })
        assertTrue(matrix.any { !it })
    }

    @Test
    fun `qr matrix is null for an empty content`() {
        assertEquals(null, qrMatrix("", sizePx = 64))
    }
}
