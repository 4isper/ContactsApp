package com.m4isper.contactsapp.data.datasource

import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildContactDetailsTest {
    @Test
    fun `extracts all field groups`() {
        val rows =
            listOf(
                phoneRow(1, "111"),
                emailRow(1, "anna@example.com"),
                addressRow(1, "Moscow, Tverskaya 1"),
                websiteRow(1, "https://example.com"),
                organizationRow(1, "ACME", "CTO"),
                noteRow(1, "Important"),
                birthdayRow(1, "1990-05-24"),
            )

        val details = buildContactDetails(header(1, "Anna"), rows)

        assertEquals("Anna", details.name)
        assertEquals(listOf("111"), details.phones.map { it.number })
        assertEquals(listOf("anna@example.com"), details.emails.map { it.address })
        assertEquals(EmailKind.HOME, details.emails.single().kind)
        assertEquals(AddressKind.HOME, details.addresses.single().kind)
        assertEquals(listOf("https://example.com"), details.websites.map { it.url })
        assertEquals("ACME", details.organization?.company)
        assertEquals("CTO", details.organization?.title)
        assertEquals("Important", details.note)
        assertEquals(Birthday(day = 24, month = 5, year = 1990), details.birthday)
    }

    @Test
    fun `reads address from formatted value`() {
        val rows =
            listOf(
                DataRow(
                    contactId = 1,
                    mimetype = StructuredPostal.CONTENT_ITEM_TYPE,
                    data1 = "Berlin, Hauptstrasse 5",
                    data2 = StructuredPostal.TYPE_WORK.toString(),
                    data4 = "Hauptstrasse 5",
                ),
            )

        val addresses = buildContactDetails(header(1, "Anna"), rows).addresses

        assertEquals("Berlin, Hauptstrasse 5", addresses.single().address)
        assertEquals(AddressKind.WORK, addresses.single().kind)
    }

    @Test
    fun `ignores events that are not birthdays`() {
        val rows = listOf(birthdayRow(1, "2024-01-01", type = Event.TYPE_ANNIVERSARY))

        assertNull(buildContactDetails(header(1, "Anna"), rows).birthday)
    }

    @Test
    fun `parses yearless birthday`() {
        val rows = listOf(birthdayRow(1, "--05-24"))

        assertEquals(Birthday(day = 24, month = 5, year = null), buildContactDetails(header(1, "Anna"), rows).birthday)
    }

    @Test
    fun `drops blank single values and returns null`() {
        val rows = listOf(noteRow(1, "   "), organizationRow(1, "  ", ""))

        val details = buildContactDetails(header(1, "Anna"), rows)

        assertNull(details.note)
        assertNull(details.organization)
    }

    @Test
    fun `extracts instant messages with known and custom protocols`() {
        val rows =
            listOf(
                imRow(1, "skype_anna", protocol = Im.PROTOCOL_SKYPE),
                imRow(1, "tg_anna", protocol = Im.PROTOCOL_CUSTOM, customProtocol = "Telegram"),
            )

        val messages = buildContactDetails(header(1, "Anna"), rows).instantMessages

        assertEquals(listOf("skype_anna", "tg_anna"), messages.map { it.address })
        assertEquals(ImProtocol.SKYPE, messages[0].protocol)
        assertEquals(null, messages[0].customProtocol)
        assertEquals(ImProtocol.CUSTOM, messages[1].protocol)
        assertEquals("Telegram", messages[1].customProtocol)
    }

    @Test
    fun `unknown im protocol falls back to custom`() {
        val rows = listOf(imRow(1, "mystery_anna", protocol = 99))

        assertEquals(
            ImProtocol.CUSTOM,
            buildContactDetails(header(1, "Anna"), rows).instantMessages.single().protocol,
        )
    }

    @Test
    fun `extracts structured name parts`() {
        val rows =
            listOf(
                nameRow(1, display = "Иван Петров", given = "Иван", family = "Петров", middle = "Петрович"),
            )

        val details = buildContactDetails(header(1, "Иван Петров"), rows)

        assertEquals(
            NameParts(given = "Иван", family = "Петров", middle = "Петрович"),
            details.nameParts,
        )
    }

    @Test
    fun `extracts phonetic name parts`() {
        val rows =
            listOf(
                nameRow(
                    1,
                    given = "Иван",
                    family = "Петров",
                    phoneticGiven = "Ivan",
                    phoneticMiddle = "Petrovich",
                    phoneticFamily = "Petrov",
                ),
            )

        val details = buildContactDetails(header(1, "Иван Петров"), rows)

        assertEquals(
            NameParts(
                given = "Иван",
                family = "Петров",
                phoneticGiven = "Ivan",
                phoneticMiddle = "Petrovich",
                phoneticFamily = "Petrov",
            ),
            details.nameParts,
        )
    }

    @Test
    fun `keeps name parts with only phonetics filled`() {
        val rows = listOf(nameRow(1, phoneticGiven = "Ivan"))

        assertEquals("Ivan", buildContactDetails(header(1, "Иван"), rows).nameParts?.phoneticGiven)
    }

    @Test
    fun `ignores structured name row without components`() {
        val rows = listOf(nameRow(1, display = "Anna"))

        assertNull(buildContactDetails(header(1, "Anna"), rows).nameParts)
    }
}
