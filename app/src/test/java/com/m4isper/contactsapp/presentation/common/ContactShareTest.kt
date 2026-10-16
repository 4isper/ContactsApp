package com.m4isper.contactsapp.presentation.common

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.Organization
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.PostalAddress
import com.m4isper.contactsapp.domain.model.Website
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactShareTest {
    @Test
    fun `builds vcard with name and typed numbers`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                phones =
                    listOf(
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
        val details =
            ContactDetails(
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

    @Test
    fun `includes email address website organization note and birthday`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                emails =
                    listOf(
                        EmailAddress("anna@example.com", EmailKind.HOME),
                        EmailAddress("a@work.com", EmailKind.WORK),
                    ),
                addresses = listOf(PostalAddress("Moscow, Tverskaya 1", AddressKind.HOME)),
                websites = listOf(Website("https://example.com")),
                organization = Organization(company = "ACME", title = "CTO"),
                note = "Important, note; here",
                birthday = Birthday(day = 24, month = 5, year = 1990),
            )

        val vcard = buildVCard(details)

        assertEquals(
            listOf(
                "EMAIL;TYPE=HOME:anna@example.com",
                "EMAIL;TYPE=WORK:a@work.com",
                "ADR;TYPE=HOME:;;Moscow\\, Tverskaya 1;;;;",
                "URL:https://example.com",
                "ORG:ACME",
                "TITLE:CTO",
                "NOTE:Important\\, note\\; here",
                "BDAY:1990-05-24",
            ),
            vcard
                .trim()
                .lines()
                .drop(3)
                .dropLast(1),
        )
    }

    @Test
    fun `writes yearless birthday and omits empty optional fields`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                birthday = Birthday(day = 24, month = 5, year = null),
            )

        val vcard = buildVCard(details)

        assertEquals(
            listOf("BDAY:--05-24"),
            vcard
                .trim()
                .lines()
                .drop(3)
                .dropLast(1),
        )
    }

    @Test
    fun `maps email and address kinds to vcard types`() {
        assertEquals("HOME", vcardEmailType(EmailKind.HOME))
        assertEquals("WORK", vcardEmailType(EmailKind.WORK))
        assertEquals("OTHER", vcardEmailType(EmailKind.OTHER))
        assertEquals("HOME", vcardAddressType(AddressKind.HOME))
        assertEquals("WORK", vcardAddressType(AddressKind.WORK))
        assertEquals("OTHER", vcardAddressType(AddressKind.OTHER))
    }
}
