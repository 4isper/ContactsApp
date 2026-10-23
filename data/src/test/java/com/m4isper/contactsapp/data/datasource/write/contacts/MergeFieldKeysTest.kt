package com.m4isper.contactsapp.data.datasource.write.contacts

import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.EditableSipAddress
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.domain.model.contact.EventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeFieldKeysTest {
    @Test
    fun phoneKey_keepsDigits() {
        assertEquals("123", phoneKey(EditablePhone(number = "1-2 3")))
        assertNull(phoneKey(EditablePhone(number = "abc")))
    }

    @Test
    fun emailKey_normalizes() {
        assertEquals("a@b.c", emailKey(EditableEmail(address = "  A@B.C  ")))
    }

    @Test
    fun addressKey_joinsParts() {
        val key = addressKey(EditableAddress(address = "a", street = "st", city = "ct"))
        assertTrue(key!!.contains("st"))
        assertTrue(key.contains("ct"))
    }

    @Test
    fun websiteKey_normalizes() {
        assertEquals("https://x.com", websiteKey(EditableWebsite(url = "  HTTPS://X.COM  ")))
    }

    @Test
    fun imKey_normalizes() {
        assertEquals("user", imKey(EditableInstantMessage(address = "  USER  ")))
    }

    @Test
    fun sipKey_normalizes() {
        assertEquals("sip:user", sipKey(EditableSipAddress(address = "  SIP:USER  ")))
    }

    @Test
    fun relationKey_normalizes() {
        assertEquals("john", relationKey(EditableRelation(name = "  JOHN  ")))
    }

    @Test
    fun eventKey_usesParsedDate() {
        val ev =
            EditableEvent(
                kind = EventKind.ANNIVERSARY,
                date =
                    com.m4isper.contactsapp.domain.model.contact
                        .Birthday(day = 1, month = 2, year = 2020),
            )
        assertEquals("ANNIVERSARY|2020-02-01", eventKey(ev))
    }

    @Test
    fun eventKey_usesUnparsedDate() {
        val ev = EditableEvent(kind = EventKind.OTHER, unparsedDate = "raw")
        assertEquals("OTHER|raw", eventKey(ev))
    }

    @Test
    fun eventKey_nullWhenNoDate() {
        assertNull(eventKey(EditableEvent(kind = EventKind.OTHER)))
    }
}
