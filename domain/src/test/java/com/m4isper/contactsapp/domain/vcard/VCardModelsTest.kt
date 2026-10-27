package com.m4isper.contactsapp.domain.vcard

import com.m4isper.contactsapp.domain.model.contact.ContactForm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class VCardModelsTest {
    @Test
    fun vCardContactEqualsAndHashCode() {
        val a =
            VCardContact(form = ContactForm(), photo = byteArrayOf(1), categories = listOf("g"), photoSkipped = true)
        val b =
            VCardContact(form = ContactForm(), photo = byteArrayOf(1), categories = listOf("g"), photoSkipped = true)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val c =
            VCardContact(form = ContactForm(), photo = byteArrayOf(2), categories = listOf("g"), photoSkipped = true)
        assertNotEquals(a, c)
    }

    @Test
    fun vCardExportContactEqualsAndHashCode() {
        val details =
            com.m4isper.contactsapp.domain.model.contact.ContactDetails(
                contactId = 1L,
                name = "n",
            )
        val a = VCardExportContact(details = details, photo = byteArrayOf(1), groups = listOf("g"))
        val b = VCardExportContact(details = details, photo = byteArrayOf(1), groups = listOf("g"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun vCardInvalidContactAndParseResultDataClasses() {
        val issue = VCardInvalidContact(name = "n", issue = VCardIssue.EMPTY)
        assertEquals(issue, VCardInvalidContact(name = "n", issue = VCardIssue.EMPTY))
        val result = VCardParseResult(contacts = emptyList(), invalid = listOf(issue))
        assertEquals(result, VCardParseResult(contacts = emptyList(), invalid = listOf(issue)))
    }
}
