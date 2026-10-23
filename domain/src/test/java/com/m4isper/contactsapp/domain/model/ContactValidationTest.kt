package com.m4isper.contactsapp.domain.model

import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.domain.model.contact.FormError
import com.m4isper.contactsapp.domain.model.contact.FormField
import com.m4isper.contactsapp.domain.model.contact.validateContactFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
