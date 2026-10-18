package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactFormValidatorTest {
    @Test
    fun `name only is valid`() {
        assertNull(
            validateContactForm(ContactForm(name = EditableName(display = "Anna"))),
        )
    }

    @Test
    fun `phone only is valid`() {
        assertNull(
            validateContactForm(
                ContactForm(
                    phones = listOf(EditablePhone(number = "111")),
                ),
            ),
        )
    }

    @Test
    fun `blank name and no phones is empty form`() {
        assertEquals(
            FormError.EMPTY_FORM,
            validateContactForm(ContactForm(name = EditableName(display = "   "))),
        )
    }

    @Test
    fun `structured given name only is valid`() {
        assertNull(
            validateContactForm(
                ContactForm(
                    name = EditableName(mode = NameMode.STRUCTURED, given = "Anna"),
                ),
            ),
        )
    }

    @Test
    fun `structured blank components is empty form`() {
        assertEquals(
            FormError.EMPTY_FORM,
            validateContactForm(
                ContactForm(
                    name = EditableName(mode = NameMode.STRUCTURED, given = "  ", family = ""),
                ),
            ),
        )
    }

    @Test
    fun `instant message only is valid`() {
        assertNull(
            validateContactForm(
                ContactForm(
                    instantMessages = listOf(EditableInstantMessage(address = "anna_tg")),
                ),
            ),
        )
    }

    @Test
    fun `blank phone row is blank phone error`() {
        assertEquals(
            FormError.BLANK_PHONE,
            validateContactForm(
                ContactForm(
                    name = EditableName(display = "Anna"),
                    phones =
                        listOf(
                            EditablePhone(number = "111"),
                            EditablePhone(number = "  "),
                        ),
                ),
            ),
        )
    }

    @Test
    fun `event with unparsed date counts as non-empty form`() {
        assertNull(
            validateContactForm(
                ContactForm(
                    events = listOf(EditableEvent(unparsedDate = "1985-03-0X")),
                ),
            ),
        )
    }
}
