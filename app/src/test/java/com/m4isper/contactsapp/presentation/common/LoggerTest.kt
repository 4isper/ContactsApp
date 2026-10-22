package com.m4isper.contactsapp.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** В лог попадает текст ошибки без цифр: там могут быть телефоны и id контактов. */
class LoggerTest {
    @Test
    fun `the digits of the error are hidden`() {
        val redacted = IllegalStateException("contact +7 900 111-22-33 is broken").redacted()

        assertFalse(redacted.contains("900"))
        assertFalse(redacted.contains("111"))
        assertEquals("java.lang.IllegalStateException: contact +# ### ###-##-## is broken", redacted)
    }

    @Test
    fun `the text of the error is limited`() {
        val redacted = IllegalStateException("a".repeat(1_000)).redacted()

        assertEquals(512, redacted.length)
    }
}
