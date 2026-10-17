package com.m4isper.contactsapp.data.datasource

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.RelationKind
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
        assertEquals(null, withoutPhoto.photoUri)

        val withPhoto =
            aggregateContacts(listOf(header(1, "Anna", photoUri = "content://photo/1")), emptyList()).single()
        assertEquals("content://photo/1", withPhoto.photoUri)
    }

    @Test
    fun `maps work mobile type to work kind`() {
        assertEquals(
            PhoneKind.WORK,
            mapPhoneKind(Phone.TYPE_WORK_MOBILE),
        )
    }

    @Test
    fun `empty input returns empty list`() {
        assertTrue(aggregateContacts(emptyList(), emptyList()).isEmpty())
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
}
