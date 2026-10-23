package com.m4isper.contactsapp.domain.model.contact

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContactValuesExtraTest {
    @Test
    fun emailEqualsHashCodeToString() {
        val a = Email.of("Anna@Example.COM")
        val b = Email.of("Anna@example.com")
        assertEquals(a, b)
        assertEquals(a?.hashCode(), b?.hashCode())
        assertEquals("Anna@example.com", a.toString())
    }

    @Test
    fun phoneEqualsHashCodeToString() {
        val a = Phone.of("+7 (999) 111-22-33")
        val b = Phone.of("+79991112233")
        assertEquals(a, b)
        assertEquals(a?.hashCode(), b?.hashCode())
        assertEquals("+79991112233", a.toString())
    }

    @Test
    fun urlEqualsHashCodeToString() {
        val a = Url.of("Example.COM")
        val b = Url.of("https://example.com")
        assertEquals(a, b)
        assertEquals(a?.hashCode(), b?.hashCode())
        assertEquals("https://example.com", a.toString())
    }
}
