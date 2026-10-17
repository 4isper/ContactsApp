package com.m4isper.contactsapp.presentation.common

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.InstantMessage
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.model.Organization
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.PostalAddress
import com.m4isper.contactsapp.domain.model.Website
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `writes instant messages as IMPP with service type`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                instantMessages =
                    listOf(
                        InstantMessage("skype_anna", ImProtocol.SKYPE),
                        InstantMessage("tg_anna", ImProtocol.CUSTOM, "Telegram"),
                        InstantMessage("aim_anna", ImProtocol.AIM),
                    ),
            )

        val vcard = buildVCard(details)

        val lines = vcard.trim().lines()
        assertEquals("IMPP;X-SERVICE-TYPE=Skype:skype_anna", lines[3])
        assertEquals("IMPP;X-SERVICE-TYPE=Telegram:tg_anna", lines[4])
        assertEquals("IMPP;X-SERVICE-TYPE=AIM:aim_anna", lines[5])
        assertEquals("Custom", vcardImService(InstantMessage("x", ImProtocol.CUSTOM)))
    }

    @Test
    fun `builds one vcard block per contact for batch share`() {
        val contacts =
            listOf(
                ContactDetails(contactId = 1, name = "Anna"),
                ContactDetails(contactId = 2, name = "Bob"),
            )

        val vcards = buildVCards(contacts)

        assertEquals(2, Regex("BEGIN:VCARD").findAll(vcards).count())
        assertEquals(2, Regex("END:VCARD").findAll(vcards).count())
        assertTrue(vcards.contains("FN:Anna"))
        assertTrue(vcards.contains("FN:Bob"))
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

    @Test
    fun `writes N line from structured name parts`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Иван Петрович Петров",
                nameParts =
                    NameParts(
                        given = "Иван",
                        family = "Петров",
                        middle = "Петрович",
                        prefix = "г-н",
                        suffix = "Jr.",
                    ),
            )

        val vcard = buildVCard(details)

        assertEquals(
            "N:Петров;Иван;Петрович;г-н;Jr.",
            vcard.lines().first { it.startsWith("N:") },
        )
    }

    @Test
    fun `escapes special chars in N line parts`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Doe, John",
                nameParts = NameParts(given = "John;Jr", family = "Doe, Sr"),
            )

        val vcard = buildVCard(details)

        assertEquals(
            "N:Doe\\, Sr;John\\;Jr;;;",
            vcard.lines().first { it.startsWith("N:") },
        )
    }

    @Test
    fun `omits N line without structured name parts`() {
        val details = ContactDetails(contactId = 7, name = "Anna")

        val vcard = buildVCard(details)

        assertEquals(false, vcard.lines().any { it.startsWith("N:") })
    }
}
