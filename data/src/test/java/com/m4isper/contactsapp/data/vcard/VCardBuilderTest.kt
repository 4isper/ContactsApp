package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactEvent
import com.m4isper.contactsapp.domain.model.contact.ContactRelation
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.EventKind
import com.m4isper.contactsapp.domain.model.contact.ImProtocol
import com.m4isper.contactsapp.domain.model.contact.InstantMessage
import com.m4isper.contactsapp.domain.model.contact.NameParts
import com.m4isper.contactsapp.domain.model.contact.Organization
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.model.contact.PostalAddress
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import com.m4isper.contactsapp.domain.model.contact.SipAddress
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.Website
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import com.m4isper.contactsapp.domain.vcard.VCardExportContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Контакт для экспорта: детали без фото и групп. */
private fun vcard(details: ContactDetails): String = buildVCard(VCardExportContact(details = details))

/** Поток .vcf из контактов без фото и групп. */
private fun vcards(details: List<ContactDetails>): String =
    buildVCards(details.map { VCardExportContact(details = it) })

class VCardBuilderTest {
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

        val vcard = vcard(details)

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

        val vcard = vcard(details)

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

        val vcard = vcard(details)

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

        val vcard = vcard(details)

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

        val vcard = vcard(details)

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

        val vcards = vcards(contacts)

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

        val vcard = vcard(details)

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

        val vcard = vcard(details)

        assertEquals(
            "N:Doe\\, Sr;John\\;Jr;;;",
            vcard.lines().first { it.startsWith("N:") },
        )
    }

    @Test
    fun `omits N line without structured name parts`() {
        val details = ContactDetails(contactId = 7, name = "Anna")

        val vcard = vcard(details)

        assertEquals(false, vcard.lines().any { it.startsWith("N:") })
    }

    @Test
    fun `writes nickname relations and events`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                nickname = "Нюша",
                relations =
                    listOf(
                        ContactRelation(name = "Bob", kind = RelationKind.FRIEND),
                        ContactRelation(name = "Михалыч", kind = RelationKind.OTHER, customLabel = "Кум"),
                    ),
                events =
                    listOf(
                        ContactEvent(
                            date = Birthday(day = 1, month = 6, year = 2015),
                            kind = EventKind.ANNIVERSARY,
                        ),
                        ContactEvent(
                            date = Birthday(day = 10, month = 9, year = null),
                            kind = EventKind.OTHER,
                            customLabel = "Свадьба",
                        ),
                    ),
            )

        val vcard = vcard(details)

        assertEquals(
            listOf(
                "NICKNAME:Нюша",
                "RELATED;TYPE=FRIEND:Bob",
                "item1.RELATED;TYPE=OTHER:Михалыч",
                "item1.X-ABLabel:Кум",
                "ANNIVERSARY:2015-06-01",
                "item2.X-ABDATE:--09-10",
                "item2.X-ABLabel:Свадьба",
            ),
            vcard
                .trim()
                .lines()
                .drop(3)
                .dropLast(1),
        )
    }

    @Test
    fun `writes custom labels of contacts as item groups`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                phones = listOf(PhoneNumber("111", PhoneKind.OTHER, customLabel = "Рабочий")),
                emails = listOf(EmailAddress("a@x.com", EmailKind.OTHER, customLabel = "Личная")),
                addresses = listOf(PostalAddress("Moscow 1", AddressKind.OTHER, customLabel = "Дача")),
            )

        val vcard = vcard(details)

        assertEquals(
            listOf(
                "item1.TEL;TYPE=VOICE:111",
                "item1.X-ABLabel:Рабочий",
                "item2.EMAIL;TYPE=OTHER:a@x.com",
                "item2.X-ABLabel:Личная",
                "item3.ADR;TYPE=OTHER:;;Moscow 1;;;;",
                "item3.X-ABLabel:Дача",
            ),
            vcard
                .trim()
                .lines()
                .drop(3)
                .dropLast(1),
        )
    }

    @Test
    fun `writes structured address primary flags sip and extended kinds`() {
        val details =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                phones =
                    listOf(
                        PhoneNumber("111", PhoneKind.MOBILE, isPrimary = true),
                        PhoneNumber("222", PhoneKind.COMPANY_MAIN),
                    ),
                emails =
                    listOf(
                        EmailAddress("anna@example.com", EmailKind.MOBILE, isPrimary = true),
                    ),
                sipAddresses =
                    listOf(
                        SipAddress("sip:anna@example.com", SipKind.WORK),
                    ),
                addresses =
                    listOf(
                        PostalAddress(
                            address = "Lenina 1, Moscow",
                            kind = AddressKind.WORK,
                            poBox = "PO 5",
                            neighborhood = "Center",
                            street = "Lenina 1",
                            city = "Moscow",
                            region = "MO",
                            postcode = "101000",
                            country = "Russia",
                        ),
                    ),
                websites = listOf(Website("https://blog.example", WebsiteKind.BLOG)),
                organization =
                    Organization(
                        company = "ACME",
                        title = "CTO",
                        department = "Platform",
                        jobDescription = "Leads engineering",
                        officeLocation = "London",
                    ),
            )

        val vcard = vcard(details)

        assertEquals(
            listOf(
                "TEL;TYPE=CELL,PREF:111",
                "TEL;TYPE=COMPANY_MAIN:222",
                "EMAIL;TYPE=CELL,PREF:anna@example.com",
                "IMPP;X-SERVICE-TYPE=SIP;TYPE=WORK:sip:anna@example.com",
                "ADR;TYPE=WORK:PO 5;Center;Lenina 1;Moscow;MO;101000;Russia",
                "URL;TYPE=BLOG:https://blog.example",
                "ORG:ACME;Platform",
                "TITLE:CTO",
                "ROLE:Leads engineering",
                "X-OFFICE-LOCATION:London",
            ),
            vcard
                .trim()
                .lines()
                .drop(3)
                .dropLast(1),
        )
    }

    @Test
    fun `maps extended kinds to vcard types`() {
        assertEquals("COMPANY_MAIN", vcardType(PhoneKind.COMPANY_MAIN))
        assertEquals("FAX", vcardType(PhoneKind.FAX))
        assertEquals("PAGER", vcardType(PhoneKind.PAGER))
        assertEquals("CALLBACK", vcardType(PhoneKind.CALLBACK))
        assertEquals("WORK,CELL", vcardType(PhoneKind.WORK_MOBILE))
        assertEquals("CELL", vcardEmailType(EmailKind.MOBILE))
        assertEquals("BLOG", vcardWebsiteType(WebsiteKind.BLOG))
        assertEquals(null, vcardWebsiteType(WebsiteKind.HOMEPAGE))
        assertEquals("WORK", vcardSipType(SipKind.WORK))
    }

    @Test
    fun `escapes carriage returns and urls`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "line1\r\nline2\rline3",
                websites = listOf(Website("https://x.dev/a,b")),
            )

        val vcard = vcard(details)

        assertEquals("FN:line1\\nline2\\nline3", vcard.lines().first { it.startsWith("FN:") })
        assertEquals("URL:https://x.dev/a\\,b", vcard.lines().first { it.startsWith("URL:") })
    }

    @Test
    fun `writes photo and categories`() {
        val details = ContactDetails(contactId = 1, name = "Anna")

        val vcard =
            buildVCard(
                VCardExportContact(
                    details = details,
                    photo = byteArrayOf(1, 2, 3),
                    groups = listOf("Friends", "Work, Home"),
                ),
            )

        assertEquals(
            "PHOTO;ENCODING=b;TYPE=JPEG:AQID",
            vcard.lines().first { it.startsWith("PHOTO") },
        )
        assertEquals(
            "CATEGORIES:Friends,Work\\, Home",
            vcard.lines().first { it.startsWith("CATEGORIES:") },
        )
    }

    @Test
    fun `photo without bytes is not written`() {
        val details = ContactDetails(contactId = 1, name = "Anna")

        val vcard = buildVCard(VCardExportContact(details = details, photo = byteArrayOf()))

        assertTrue(vcard.lines().none { it.startsWith("PHOTO") })
    }
}
