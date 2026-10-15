package com.m4isper.contactsapp.presentation.common

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactShareTest {

    @Test
    fun `builds vcard with name and typed numbers`() {
        val details = ContactDetails(
            contactId = 7,
            name = "Anna",
            phones = listOf(
                PhoneNumber("111", PhoneKind.MOBILE),
                PhoneNumber("222", PhoneKind.HOME),
                PhoneNumber("333", PhoneKind.WORK),
                PhoneNumber("444", PhoneKind.OTHER),
            ),
        )

        val vcard = buildVCard(details)

        assertEquals(
            "BEGIN:VCARD\n" +
                "VERSION:3.0\n" +
                "FN:Anna\n" +
                "TEL;TYPE=CELL:111\n" +
                "TEL;TYPE=HOME:222\n" +
                "TEL;TYPE=WORK:333\n" +
                "TEL;TYPE=VOICE:444\n" +
                "END:VCARD\n",
            vcard,
        )
    }

    @Test
    fun `escapes special chars in name`() {
        val details = ContactDetails(
            contactId = 1,
            name = "Doe, John; Jr\n III\\Sr",
            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
        )

        val vcard = buildVCard(details)

        assertEquals(
            "FN:Doe\\, John\\; Jr\\n III\\\\Sr",
            vcard.lines().first { it.startsWith("FN:") },
        )
    }

    @Test
    fun `maps phone kinds to vcard types`() {
        assertEquals("CELL", vcardType(PhoneKind.MOBILE))
        assertEquals("HOME", vcardType(PhoneKind.HOME))
        assertEquals("WORK", vcardType(PhoneKind.WORK))
        assertEquals("VOICE", vcardType(PhoneKind.OTHER))
    }
}
