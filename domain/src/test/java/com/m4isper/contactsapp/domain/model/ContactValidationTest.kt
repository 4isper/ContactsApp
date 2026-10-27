package com.m4isper.contactsapp.domain.model

import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.domain.model.contact.FormError
import com.m4isper.contactsapp.domain.model.contact.FormField
import com.m4isper.contactsapp.domain.model.contact.firstInvalidRowIndex
import com.m4isper.contactsapp.domain.model.contact.validateContactFields
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactValidationTest {
    @Test
    fun `valid fields produce no errors`() {
        val form =
            ContactForm(
                name = EditableName(display = "Anna"),
                phones = listOf(EditablePhone(number = "+79161234567")),
                emails = listOf(EditableEmail(address = "anna@example.com")),
                websites = listOf(EditableWebsite(url = "https://example.com")),
            )

        assertTrue(validateContactFields(form).isEmpty())
    }

    @Test
    fun `phone without digits is invalid`() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "нет")))

        assertEquals(FormError.INVALID_PHONE, validateContactFields(form)[FormField.PHONE])
    }

    @Test
    fun `email without domain is invalid`() {
        val form = ContactForm(emails = listOf(EditableEmail(address = "anna@")))

        assertEquals(FormError.INVALID_EMAIL, validateContactFields(form)[FormField.EMAIL])
    }

    @Test
    fun `website without host is invalid`() {
        val form = ContactForm(websites = listOf(EditableWebsite(url = "example")))

        assertEquals(FormError.INVALID_URL, validateContactFields(form)[FormField.WEBSITE])
    }

    @Test
    fun `short phone is allowed`() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "123")))

        assertTrue(validateContactFields(form).isEmpty())
    }

    @Test
    fun `unparsed event date is reported`() {
        val form = ContactForm(events = listOf(EditableEvent(unparsedDate = "1985-03-0X")))

        assertEquals(FormError.INVALID_DATE, validateContactFields(form)[FormField.EVENT])
    }

    @Test
    fun `first invalid row index points at the offending row`() {
        val form =
            ContactForm(
                phones = listOf(EditablePhone(number = "+79161234567"), EditablePhone(number = "нет")),
                emails = listOf(EditableEmail(address = "anna@"), EditableEmail(address = "ok@example.com")),
                events = listOf(EditableEvent(unparsedDate = "??")),
            )

        assertEquals(1, firstInvalidRowIndex(form, FormField.PHONE))
        assertEquals(0, firstInvalidRowIndex(form, FormField.EMAIL))
        assertEquals(0, firstInvalidRowIndex(form, FormField.EVENT))
        assertNull(firstInvalidRowIndex(form, FormField.WEBSITE))
    }

    @Test
    fun `blank values and valid rows yield no index`() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "")))

        assertNull(firstInvalidRowIndex(form, FormField.PHONE))
    }
}
