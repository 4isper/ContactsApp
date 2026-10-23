package com.m4isper.contactsapp.domain.vcard

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VCardModelsFullTest {
    @Test
    fun vCardContactEqualsHashCodeToString() {
        val a =
            VCardContact(
                form = ContactForm(),
                photo = byteArrayOf(1),
                categories = listOf("g"),
                photoSkipped = true,
            )
        val b =
            VCardContact(
                form = ContactForm(),
                photo = byteArrayOf(1),
                categories = listOf("g"),
                photoSkipped = true,
            )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a.toString().isNotEmpty())

        val c =
            VCardContact(form = ContactForm(), photo = byteArrayOf(2), categories = listOf("g"), photoSkipped = true)
        assertNotEquals(a, c)
    }

    @Test
    fun vCardContact_nullPhoto() {
        val a = VCardContact(form = ContactForm(), photo = null)
        val b = VCardContact(form = ContactForm(), photo = null)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun vCardExportContactEqualsHashCodeToString() {
        val details = ContactDetails(contactId = 1L, name = "n")
        val a = VCardExportContact(details = details, photo = byteArrayOf(1), groups = listOf("g"))
        val b = VCardExportContact(details = details, photo = byteArrayOf(1), groups = listOf("g"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertTrue(a.toString().isNotEmpty())
    }

    @Test
    fun vCardIssueValues() {
        assertEquals(2, VCardIssue.values().size)
        assertEquals(VCardIssue.EMPTY, VCardIssue.valueOf("EMPTY"))
        assertEquals(VCardIssue.UNSUPPORTED, VCardIssue.valueOf("UNSUPPORTED"))
    }

    @Test
    fun vCardInvalidContact() {
        val issue = VCardInvalidContact(name = "n", issue = VCardIssue.EMPTY)
        assertEquals(issue, VCardInvalidContact(name = "n", issue = VCardIssue.EMPTY))
        assertEquals(issue.hashCode(), VCardInvalidContact(name = "n", issue = VCardIssue.EMPTY).hashCode())
        assertTrue(issue.toString().isNotEmpty())
    }

    @Test
    fun vCardParseResult() {
        val result = VCardParseResult(contacts = emptyList(), invalid = emptyList())
        assertEquals(result, VCardParseResult(contacts = emptyList(), invalid = emptyList()))
        assertEquals(result.hashCode(), VCardParseResult().hashCode())
        assertTrue(result.toString().isNotEmpty())
    }
}
