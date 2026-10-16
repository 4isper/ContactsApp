package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactFormValidatorTest {
    @Test
    fun `name only is valid`() {
        assertNull(
            validateContactForm(ContactForm(name = "Anna")),
        )
    }

    @Test
    fun `phone only is valid`() {
        assertNull(
            validateContactForm(
                ContactForm(
                    name = "",
                    phones = listOf(EditablePhone(number = "111")),
                ),
            ),
        )
    }

    @Test
    fun `blank name and no phones is empty form`() {
        assertEquals(
            FormError.EMPTY_FORM,
            validateContactForm(ContactForm(name = "   ")),
        )
    }

    @Test
    fun `blank phone row is blank phone error`() {
        assertEquals(
            FormError.BLANK_PHONE,
            validateContactForm(
                ContactForm(
                    name = "Anna",
                    phones =
                        listOf(
                            EditablePhone(number = "111"),
                            EditablePhone(number = "  "),
                        ),
                ),
            ),
        )
    }
}
