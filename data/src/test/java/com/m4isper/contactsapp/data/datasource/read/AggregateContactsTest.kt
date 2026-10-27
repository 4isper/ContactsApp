package com.m4isper.contactsapp.data.datasource.read

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.SipAddress
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AggregateContactsTest {
    @Test
    fun `groups phone rows by contact id`() {
        val headers = listOf(header(1, "Anna"), header(2, "Bob"))
        val rows =
            listOf(
                phoneRow(1, "111"),
                phoneRow(1, "112", Phone.TYPE_HOME),
                phoneRow(2, "222"),
            )

        val result = aggregateContacts(headers, rows)

        assertEquals(2, result.size)
        val anna = result.first { it.contactId == 1L }
        assertEquals("Anna", anna.name)
        assertEquals(listOf("111", "112"), anna.phones.map { it.number })
        assertEquals(
            listOf(PhoneKind.MOBILE, PhoneKind.HOME),
            anna.phones.map { it.kind },
        )
    }

    @Test
    fun `keeps contacts without phones and emails`() {
        val headers = listOf(header(1, "Anna"), header(2, "Bob"))
        val rows = listOf(phoneRow(1, "111"))

        val result = aggregateContacts(headers, rows)

        assertEquals(listOf(1L, 2L), result.map { it.contactId })
        assertTrue(result.first { it.contactId == 2L }.phones.isEmpty())
        assertEquals(null, result.first { it.contactId == 2L }.nameParts)
    }

    @Test
    fun `reads name parts from structured name rows`() {
        val headers = listOf(header(1, "Петров Иван"))
        val rows =
            listOf(
                nameRow(1, display = "Петров Иван", given = "Иван", family = "Петров", middle = "Петрович"),
            )

        val nameParts = aggregateContacts(headers, rows).single().nameParts

        assertEquals("Иван", nameParts?.given)
        assertEquals("Петров", nameParts?.family)
        assertEquals("Петрович", nameParts?.middle)
    }

    @Test
    fun `aggregates birthdays for reminder consumers`() {
        val headers = listOf(header(1, "Anna"))
        val rows = listOf(birthdayRow(1, "1990-05-24"))

        val result = aggregateContacts(headers, rows)

        assertEquals(1990, result.single().birthday?.year)
        assertEquals(5, result.single().birthday?.month)
        assertEquals(24, result.single().birthday?.day)
    }

    @Test
    fun `preserves header order`() {
        val headers = listOf(header(2, "Bob"), header(1, "Anna"))

        val result = aggregateContacts(headers, emptyList())

        assertEquals(listOf("Bob", "Anna"), result.map { it.name })
    }

    @Test
    fun `drops blank names`() {
        val headers = listOf(header(1, "   "), header(2, "Bob"), header(3, "Anna"))

        val result = aggregateContacts(headers, emptyList())

        assertEquals(listOf(2L, 3L), result.map { it.contactId })
    }

    @Test
    fun `ignores blank and deduplicates identical numbers`() {
        val headers = listOf(header(1, "Anna"))
        val rows =
            listOf(
                phoneRow(1, "   "),
                phoneRow(1, "111"),
                phoneRow(1, "111", Phone.TYPE_WORK),
            )

        val result = aggregateContacts(headers, rows)

        assertEquals(listOf("111"), result.single().phones.map { it.number })
    }

    @Test
    fun `keeps custom label only for custom type`() {
        val headers = listOf(header(1, "Anna"))
        val rows =
            listOf(
                phoneRow(1, "111", Phone.TYPE_CUSTOM, "Sister"),
                phoneRow(1, "112", Phone.TYPE_MOBILE, "Ignored"),
            )

        val phones = aggregateContacts(headers, rows).single().phones

        assertEquals("Sister", phones.first { it.number == "111" }.customLabel)
        assertEquals(null, phones.first { it.number == "112" }.customLabel)
    }

    @Test
    fun `includes and deduplicates emails`() {
        val headers = listOf(header(1, "Anna"))
        val rows =
            listOf(
                emailRow(1, "anna@example.com", Email.TYPE_HOME),
                emailRow(1, "anna@example.com", Email.TYPE_WORK),
                emailRow(1, "other@example.com", Email.TYPE_CUSTOM, "Side"),
            )

        val emails = aggregateContacts(headers, rows).single().emails

        assertEquals(
            listOf("anna@example.com", "other@example.com"),
            emails.map { it.address },
        )
        assertEquals("Side", emails.last().customLabel)
    }

    @Test
    fun `takes photo uri from header`() {
        val withoutPhoto = aggregateContacts(listOf(header(1, "Anna")), emptyList()).single()
        assertEquals(null, withoutPhoto.photo)

        val withPhoto =
            aggregateContacts(listOf(header(1, "Anna", photoUri = "content://photo/1")), emptyList()).single()
        assertEquals("content://photo/1", withPhoto.photo?.uri)
    }

    @Test
    fun `maps work mobile type to work kind`() {
        assertEquals(
            PhoneKind.WORK_MOBILE,
            mapPhoneKind(Phone.TYPE_WORK_MOBILE),
        )
    }

    @Test
    fun `empty input returns empty list`() {
        assertTrue(aggregateContacts(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `includes extended search fields account and last update`() {
        val google = ContactAccount(name = "user@gmail.com", type = "com.google")
        val headers = listOf(header(1, "Anna", lastUpdated = 123L))
        val rows =
            listOf(
                organizationRow(1, "ACME", "CTO"),
                addressRow(1, "Moscow, Tverskaya 1"),
                websiteRow(1, "https://example.com"),
                noteRow(1, "Important"),
            )

        val details = aggregateContacts(headers, rows, mapOf(1L to google)).single()

        assertEquals("ACME", details.organization?.company)
        assertEquals("CTO", details.organization?.title)
        assertEquals(listOf("Moscow, Tverskaya 1"), details.addresses.map { it.address })
        assertEquals(listOf("https://example.com"), details.websites.map { it.url })
        assertEquals("Important", details.note)
        assertEquals(google, details.account)
        assertEquals(123L, details.lastUpdated)
    }

    @Test
    fun `includes device last contacted fields`() {
        val headers = listOf(header(1, "Anna", lastContacted = 1_700_000_000_000L))

        val details = aggregateContacts(headers, emptyList()).single()

        assertEquals(1_700_000_000_000L, details.lastContacted)
    }

    @Test
    fun `includes nickname and relations for search`() {
        val headers = listOf(header(1, "Anna"))
        val rows =
            listOf(
                nicknameRow(1, "Нюша"),
                relationRow(1, "Михалыч", Relation.TYPE_CUSTOM, "Кум"),
                relationRow(1, "Bob", Relation.TYPE_FRIEND),
            )

        val details = aggregateContacts(headers, rows).single()

        assertEquals("Нюша", details.nickname)
        assertEquals(listOf("Михалыч", "Bob"), details.relations.map { it.name })
        assertEquals(RelationKind.OTHER, details.relations[0].kind)
        assertEquals("Кум", details.relations[0].customLabel)
    }

    @Test
    fun `uses explicitly primary phone and email`() {
        val headers = listOf(header(1, "Anna"))
        val rows =
            listOf(
                phoneRow(1, "111"),
                phoneRow(1, "222", Phone.TYPE_WORK, isPrimary = true),
                emailRow(1, "home@example.com"),
                emailRow(1, "work@example.com", Email.TYPE_WORK, isPrimary = true),
            )

        val details = aggregateContacts(headers, rows).single()

        assertEquals("222", details.primaryPhone?.number)
        assertEquals(true, details.primaryPhone?.isPrimary)
        assertEquals("work@example.com", details.primaryEmail?.address)
        assertEquals(true, details.primaryEmail?.isPrimary)
    }

    @Test
    fun `maps structured address parts and keeps legacy fallback`() {
        val headers = listOf(header(1, "Anna"), header(2, "Bob"))
        val rows =
            listOf(
                addressRow(
                    contactId = 1,
                    address = "PO 5, Lenina 1, Center, Moscow, MO, 101000, Russia",
                    type = StructuredPostal.TYPE_WORK,
                    label = null,
                    street = "Lenina 1",
                    poBox = "PO 5",
                    neighborhood = "Center",
                    city = "Moscow",
                    region = "MO",
                    postcode = "101000",
                    country = "Russia",
                ),
                addressRow(2, "Old one-line address"),
            )

        val result = aggregateContacts(headers, rows)

        val structured = result.first { it.contactId == 1L }.addresses.single()
        assertEquals(AddressKind.WORK, structured.kind)
        assertEquals("Lenina 1", structured.street)
        assertEquals("PO 5", structured.poBox)
        assertEquals("Center", structured.neighborhood)
        assertEquals("Moscow", structured.city)
        assertEquals("MO", structured.region)
        assertEquals("101000", structured.postcode)
        assertEquals("Russia", structured.country)
        assertEquals(
            "PO 5, Lenina 1, Center, Moscow, MO, 101000, Russia",
            structured.displayAddress,
        )

        val legacy = result.first { it.contactId == 2L }.addresses.single()
        assertEquals("Old one-line address", legacy.address)
        assertEquals("Old one-line address", legacy.displayAddress)
    }

    @Test
    fun `maps extended organization fields`() {
        val headers = listOf(header(1, "Anna"))
        val rows =
            listOf(
                organizationRow(
                    contactId = 1,
                    company = "ACME",
                    title = "CTO",
                    department = "Platform",
                    jobDescription = "Leads engineering",
                    officeLocation = "London",
                ),
            )

        val organization = aggregateContacts(headers, rows).single().organization

        assertEquals("ACME", organization?.company)
        assertEquals("CTO", organization?.title)
        assertEquals("Platform", organization?.department)
        assertEquals("Leads engineering", organization?.jobDescription)
        assertEquals("London", organization?.officeLocation)
    }

    @Test
    fun `maps extended kinds websites and sip addresses`() {
        val headers = listOf(header(1, "Anna"))
        val rows =
            listOf(
                phoneRow(1, "111", Phone.TYPE_WORK_MOBILE),
                phoneRow(1, "222", Phone.TYPE_FAX_HOME),
                emailRow(1, "mobile@example.com", Email.TYPE_MOBILE),
                websiteRow(1, "https://blog.example", Website.TYPE_BLOG),
                sipRow(1, "sip:anna@example.com", type = SipAddress.TYPE_HOME),
                sipRow(1, "sip:anna@work.example", type = SipAddress.TYPE_WORK, label = null),
            )

        val details = aggregateContacts(headers, rows).single()

        assertEquals(
            listOf(PhoneKind.WORK_MOBILE, PhoneKind.FAX),
            details.phones.map { it.kind },
        )
        assertEquals(EmailKind.MOBILE, details.emails.single().kind)
        assertEquals(WebsiteKind.BLOG, details.websites.single().kind)
        assertEquals(
            listOf("sip:anna@example.com", "sip:anna@work.example"),
            details.sipAddresses.map { it.address },
        )
        assertEquals(SipKind.HOME, details.sipAddresses[0].kind)
        assertEquals(SipKind.WORK, details.sipAddresses[1].kind)
    }
}
