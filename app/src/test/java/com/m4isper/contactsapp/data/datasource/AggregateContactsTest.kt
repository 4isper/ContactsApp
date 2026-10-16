package com.m4isper.contactsapp.data.datasource

import android.provider.ContactsContract
import com.m4isper.contactsapp.domain.model.PhoneKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AggregateContactsTest {
    private fun row(
        contactId: Long,
        name: String,
        phone: String,
        type: Int = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
        label: String? = null,
    ) = PhoneRow(
        contactId = contactId,
        name = name,
        phone = phone,
        type = type,
        label = label,
    )

    @Test
    fun `groups rows by contact id`() {
        val rows =
            listOf(
                row(1, "Anna", "111"),
                row(1, "Anna", "112", ContactsContract.CommonDataKinds.Phone.TYPE_HOME),
                row(2, "Bob", "222"),
            )

        val result = aggregateContacts(rows)

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
    fun `sorts contacts by name`() {
        val rows =
            listOf(
                row(2, "Bob", "222"),
                row(1, "Anna", "111"),
            )

        val result = aggregateContacts(rows)

        assertEquals(listOf("Anna", "Bob"), result.map { it.name })
    }

    @Test
    fun `drops blank names and phones`() {
        val rows =
            listOf(
                row(1, "   ", "111"),
                row(2, "Bob", "   "),
                row(3, "Anna", "113"),
            )

        val result = aggregateContacts(rows)

        assertEquals(listOf(3L), result.map { it.contactId })
    }

    @Test
    fun `deduplicates identical numbers`() {
        val rows =
            listOf(
                row(1, "Anna", "111"),
                row(1, "Anna", "111", ContactsContract.CommonDataKinds.Phone.TYPE_WORK),
            )

        val result = aggregateContacts(rows)

        assertEquals(1, result.single().phones.size)
    }

    @Test
    fun `keeps custom label only for custom type`() {
        val rows =
            listOf(
                row(
                    1,
                    "Anna",
                    "111",
                    ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
                    "Sister",
                ),
                row(
                    1,
                    "Anna",
                    "112",
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                    "Ignored",
                ),
            )

        val phones = aggregateContacts(rows).single().phones

        assertEquals("Sister", phones.first { it.number == "111" }.customLabel)
        assertEquals(null, phones.first { it.number == "112" }.customLabel)
    }

    @Test
    fun `maps work mobile type to work kind`() {
        assertEquals(
            PhoneKind.WORK,
            mapPhoneKind(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE),
        )
    }

    @Test
    fun `takes first non-null photo uri`() {
        val rows =
            listOf(
                row(1, "Anna", "111"),
                row(1, "Anna", "112"),
            )

        val withoutPhoto = aggregateContacts(rows).single()
        assertEquals(null, withoutPhoto.photoUri)

        val withPhoto =
            aggregateContacts(
                listOf(
                    row(1, "Anna", "111"),
                    row(1, "Anna", "112").copy(photoUri = "content://photo/1"),
                ),
            ).single()
        assertEquals("content://photo/1", withPhoto.photoUri)
    }

    @Test
    fun `empty rows return empty list`() {
        assertTrue(aggregateContacts(emptyList()).isEmpty())
    }
}
