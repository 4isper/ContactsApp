package com.m4isper.contactsapp.domain.model.contact

import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.Email
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.EventKind
import com.m4isper.contactsapp.domain.model.contact.Phone
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.Url
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactValuesFullTest {
    @Test
    fun emailAllValidInvalid() {
        assertEquals("A@b.co", Email.of("  A@B.CO  ")?.value)
        assertNull(Email.of("anna@"))
        assertNull(Email.of("@example.com"))
        assertNull(Email.of("anna@example"))
        assertNull(Email.of("anna@@example.com"))
        assertNull(Email.of("anna@example..com"))
        assertNull(Email.of("anna@-example.com"))
        assertNull(Email.of("anna@example.c"))
        assertNull(Email.of(""))
    }

    @Test
    fun emailDomainLowercased() {
        assertEquals("Anna@example.com", Email.of("Anna@Example.COM")?.value)
    }

    @Test
    fun phoneAllValidInvalid() {
        assertEquals("123", Phone.of("123")?.value)
        assertEquals("+79991112233", Phone.of("+79991112233")?.value)
        assertEquals("84951234567", Phone.of("8 495 123 45 67")?.value)
        assertEquals("+79991112233", Phone.of("+7 (999) 111-22-33")?.value)
        assertNull(Phone.of("12"))
        assertNull(Phone.of("call-me"))
        assertNull(Phone.of("7999abc"))
        assertNull(Phone.of("+"))
        assertNull(Phone.of(""))
    }

    @Test
    fun urlAllValidInvalid() {
        assertEquals("https://example.com", Url.of("example.com")?.value)
        assertEquals("http://example.com", Url.of("HTTP://Example.COM")?.value)
        assertEquals("https://example.com:8080/path", Url.of("https://example.com:8080/path")?.value)
        assertEquals("https://xn--e1afmkfd.xn--p1ai", Url.of("https://пример.рф")?.value)
        assertNull(Url.of(""))
        assertNull(Url.of("ftp://example.com"))
    }

    @Test
    fun allEnumsCovered() {
        assertTrue(PhoneKind.values().isNotEmpty())
        assertTrue(EmailKind.values().isNotEmpty())
        assertTrue(AddressKind.values().isNotEmpty())
        assertTrue(WebsiteKind.values().isNotEmpty())
        assertTrue(SipKind.values().isNotEmpty())
        assertTrue(RelationKind.values().isNotEmpty())
        assertTrue(EventKind.values().isNotEmpty())
        assertTrue(PhoneKind.MOBILE.toString().isNotEmpty())
        assertTrue(EmailKind.HOME.toString().isNotEmpty())
    }
}
