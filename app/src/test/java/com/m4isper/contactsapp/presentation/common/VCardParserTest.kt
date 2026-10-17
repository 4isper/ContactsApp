package com.m4isper.contactsapp.presentation.common

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.InstantMessage
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.model.Organization
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.PostalAddress
import com.m4isper.contactsapp.domain.model.Website
import org.junit.Assert.assertEquals
import org.junit.Test

class VCardParserTest {
    @Test
    fun `round-trips our own export`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "Anna Ivanova",
                phones =
                    listOf(
                        PhoneNumber("+71112223344", PhoneKind.MOBILE),
                        PhoneNumber("222", PhoneKind.WORK),
                    ),
                emails = listOf(EmailAddress("anna@work.com", EmailKind.WORK)),
                addresses = listOf(PostalAddress("Lenina 1, Moscow", AddressKind.HOME)),
                websites = listOf(Website("https://anna.dev")),
                instantMessages = listOf(InstantMessage("skype:anna", ImProtocol.SKYPE)),
                organization = Organization("Acme", "QA"),
                note = "hi",
                birthday = Birthday(day = 5, month = 4, year = 1990),
                nameParts = NameParts(given = "Anna", family = "Ivanova"),
            )

        val forms = parseVCards(buildVCards(listOf(details)))

        assertEquals(1, forms.size)
        val form = forms.single()
        assertEquals(NameMode.STRUCTURED, form.name.mode)
        assertEquals("Anna", form.name.given)
        assertEquals("Ivanova", form.name.family)
        assertEquals(listOf("+71112223344", "222"), form.phones.map { it.number })
        assertEquals(listOf(PhoneKind.MOBILE, PhoneKind.WORK), form.phones.map { it.kind })
        assertEquals("anna@work.com", form.emails.single().address)
        assertEquals(EmailKind.WORK, form.emails.single().kind)
        assertEquals("Lenina 1, Moscow", form.addresses.single().address)
        assertEquals(AddressKind.HOME, form.addresses.single().kind)
        assertEquals("https://anna.dev", form.websites.single().url)
        assertEquals(ImProtocol.SKYPE, form.instantMessages.single().protocol)
        assertEquals("skype:anna", form.instantMessages.single().address)
        assertEquals("Acme", form.organization.company)
        assertEquals("QA", form.organization.title)
        assertEquals("hi", form.note.note)
        assertEquals(Birthday(day = 5, month = 4, year = 1990), form.birthday.value)
    }

    @Test
    fun `parses display name only`() {
        val form = parseVCards("BEGIN:VCARD\nVERSION:3.0\nFN:Bob\nEND:VCARD").single()

        assertEquals(NameMode.DISPLAY, form.name.mode)
        assertEquals("Bob", form.name.display)
    }

    @Test
    fun `parses vcard 4 point 0 lowercase types`() {
        val form =
            parseVCards(
                """
                begin:vcard
                version:4.0
                fn:Cara
                tel;TYPE=cell:+1
                email;TYPE=work:cara@x.com
                end:vcard
                """.trimIndent(),
            ).single()

        assertEquals(PhoneKind.MOBILE, form.phones.single().kind)
        assertEquals(EmailKind.WORK, form.emails.single().kind)
    }

    @Test
    fun `joins folded lines`() {
        val form = parseVCards("BEGIN:VCARD\nFN:Bob\nNOTE:first\n part\nEND:VCARD").single()

        assertEquals("firstpart", form.note.note)
    }

    @Test
    fun `parses several cards and skips empty ones`() {
        val forms =
            parseVCards(
                """
                BEGIN:VCARD
                VERSION:3.0
                FN:Anna
                END:VCARD
                BEGIN:VCARD
                VERSION:3.0
                END:VCARD
                BEGIN:VCARD
                VERSION:3.0
                FN:Bob
                TEL;TYPE=HOME:222
                END:VCARD
                """.trimIndent(),
            )

        assertEquals(listOf("Anna", "Bob"), forms.map { it.name.display })
    }

    @Test
    fun `strips group prefixes and tel uri scheme`() {
        val form =
            parseVCards(
                """
                BEGIN:VCARD
                FN:Dave
                item1.TEL;TYPE=CELL:tel:+79990001122
                item1.X-ABLabel:Мобильный
                END:VCARD
                """.trimIndent(),
            ).single()

        assertEquals("+79990001122", form.phones.single().number)
    }

    @Test
    fun `parses birthday variants`() {
        val yearless = parseVCards("BEGIN:VCARD\nFN:A\nBDAY:--04-05\nEND:VCARD").single()
        assertEquals(Birthday(day = 5, month = 4, year = null), yearless.birthday.value)

        val full = parseVCards("BEGIN:VCARD\nFN:B\nBDAY:1990-04-05T00:00:00Z\nEND:VCARD").single()
        assertEquals(Birthday(day = 5, month = 4, year = 1990), full.birthday.value)
    }

    @Test
    fun `parses instant messengers with service types`() {
        val form =
            parseVCards(
                """
                BEGIN:VCARD
                FN:Eve
                IMPP;X-SERVICE-TYPE=Telegram:tg://resolve?domain=eve
                IMPP:xmpp:eve@jabber.org
                END:VCARD
                """.trimIndent(),
            ).single()

        assertEquals(ImProtocol.CUSTOM, form.instantMessages[0].protocol)
        assertEquals("Telegram", form.instantMessages[0].customProtocol)
        assertEquals(ImProtocol.JABBER, form.instantMessages[1].protocol)
    }

    @Test
    fun `parses address components and organization`() {
        val form =
            parseVCards(
                """
                BEGIN:VCARD
                FN:Frank
                ADR;TYPE=WORK:;;Lenina 1;Moscow;MO;101000;Russia
                ORG:Acme;QA
                TITLE:Lead
                END:VCARD
                """.trimIndent(),
            ).single()

        assertEquals("Lenina 1, Moscow, MO, 101000, Russia", form.addresses.single().address)
        assertEquals(AddressKind.WORK, form.addresses.single().kind)
        assertEquals("Acme", form.organization.company)
        assertEquals("Lead", form.organization.title)
    }
}
