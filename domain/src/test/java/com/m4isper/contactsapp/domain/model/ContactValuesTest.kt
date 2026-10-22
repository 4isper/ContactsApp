package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Проверка значений формы: валидные значения принимаются и нормализуются,
 * негодные отклоняются с понятным результатом.
 */
class ContactValuesTest {
    @Test
    fun `valid emails are accepted`() {
        val valid =
            listOf(
                "anna@example.com",
                "anna.petrova@example.co.uk",
                "anna+tag@example.org",
                "a@b.io",
            )

        valid.forEach { raw ->
            val email = Email.of(raw)
            assertEquals("email must be accepted: $raw", raw, email?.value)
        }
    }

    @Test
    fun `the domain of an email is lower cased`() {
        assertEquals("Anna@example.com", Email.of("Anna@Example.COM")?.value)
    }

    @Test
    fun `invalid emails are rejected`() {
        val invalid =
            listOf(
                "anna",
                "anna@",
                "@example.com",
                "anna@example",
                "anna@@example.com",
                "anna @example.com",
                "anna@example..com",
                "anna@-example.com",
                "anna@example.c",
            )

        invalid.forEach { raw ->
            assertNull("email must be rejected: $raw", Email.of(raw))
        }
    }

    @Test
    fun `valid phones are accepted`() {
        val valid =
            listOf(
                "123" to "123",
                "+79991112233" to "+79991112233",
                "8 495 123 45 67" to "84951234567",
                "+7 (999) 111-22-33" to "+79991112233",
            )

        valid.forEach { (raw, expected) ->
            assertEquals("phone must be accepted: $raw", expected, Phone.of(raw)?.value)
        }
    }

    @Test
    fun `the formatting of a phone is removed`() {
        assertEquals("+79991112233", Phone.of("+7 (999) 111-22-33")?.value)
        assertEquals("84951234567", Phone.of("8 495 123 45 67")?.value)
    }

    @Test
    fun `invalid phones are rejected`() {
        val invalid =
            listOf(
                "12",
                "+1 (234) 567-890-123-456-789",
                "call-me",
                "7999abc",
                "+",
            )

        invalid.forEach { raw ->
            assertNull("phone must be rejected: $raw", Phone.of(raw))
        }
    }

    @Test
    fun `valid urls are accepted`() {
        val valid =
            listOf(
                "https://example.com" to "https://example.com",
                "http://example.com/path?query=1" to "http://example.com/path?query=1",
                "example.com" to "https://example.com",
                "https://sub.domain.example.co.uk/page" to "https://sub.domain.example.co.uk/page",
            )

        valid.forEach { (raw, expected) ->
            assertEquals("url must be accepted: $raw", expected, Url.of(raw)?.value)
        }
    }

    @Test
    fun `the scheme and the host of an url are normalized`() {
        assertEquals("https://example.com", Url.of("example.com")?.value)
        assertEquals("http://example.com", Url.of("HTTP://Example.COM")?.value)
        assertEquals("https://example.com:8080/path", Url.of("https://example.com:8080/path")?.value)
    }

    @Test
    fun `an international host becomes ascii`() {
        assertEquals("https://xn--e1afmkfd.xn--p1ai", Url.of("https://пример.рф")?.value)
    }

    @Test
    fun `invalid urls are rejected`() {
        val invalid =
            listOf(
                "",
                "   ",
                "ftp://example.com",
                "https://",
                "https:// example.com",
                "https://-example.com",
            )

        invalid.forEach { raw ->
            assertNull("url must be rejected: $raw", Url.of(raw))
        }
    }
}
