package com.m4isper.contactsapp.data.datasource.read

import android.database.MatrixCursor
import android.provider.ContactsContract
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsRowsReaderTest {

    @Test
    fun parseContactRows_emptyCursor() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION)
        val rows = parseContactRows(cursor)
        assertTrue(rows.names.isEmpty())
        assertTrue(rows.phones.isEmpty())
        assertTrue(rows.emails.isEmpty())
        assertTrue(rows.addresses.isEmpty())
        assertTrue(rows.websites.isEmpty())
        assertTrue(rows.instantMessages.isEmpty())
        assertTrue(rows.sipAddresses.isEmpty())
        assertTrue(rows.relations.isEmpty())
        assertTrue(rows.events.isEmpty())
        assertTrue(rows.groupMemberships.isEmpty())
        assertNull(rows.organization)
        assertNull(rows.note)
        assertNull(rows.birthday)
        assertNull(rows.nickname)
    }

    @Test
    fun parseContactRows_structuredName() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    "John Doe", "John", "Doe", "Mr.", "Middle", "Suffix",
                    "Phonetic Given", "Phonetic Middle", "Phonetic Family",
                    "", 0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.names.size)
        assertEquals("John Doe", rows.names.first().display)
        assertEquals("John", rows.names.first().given)
        assertEquals("Doe", rows.names.first().family)
        assertEquals("Mr.", rows.names.first().prefix)
        assertEquals("Middle", rows.names.first().middle)
        assertEquals("Suffix", rows.names.first().suffix)
    }

    @Test
    fun parseContactRows_phone() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    "+1234567890",
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                    "Mobile", "", "", "", "", "", "", "",
                    1,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.phones.size)
        assertEquals("+1234567890", rows.phones.first().number)
        assertEquals(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, rows.phones.first().type)
        assertEquals("Mobile", rows.phones.first().label)
        assertTrue(rows.phones.first().isPrimary)
    }

    @Test
    fun parseContactRows_email() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    "john@example.com",
                    ContactsContract.CommonDataKinds.Email.TYPE_WORK,
                    "Work", "", "", "", "", "", "", "",
                    0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.emails.size)
        assertEquals("john@example.com", rows.emails.first().address)
        assertEquals(ContactsContract.CommonDataKinds.Email.TYPE_WORK, rows.emails.first().type)
        assertEquals("Work", rows.emails.first().label)
    }

    @Test
    fun parseContactRows_website() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                    "https://example.com",
                    ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE,
                    "Homepage", "", "", "", "", "", "", "",
                    0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.websites.size)
        assertEquals("https://example.com", rows.websites.first().url)
        assertEquals(ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE, rows.websites.first().type)
        assertEquals("Homepage", rows.websites.first().label)
    }

    @Test
    fun parseContactRows_sip() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
                    "sip:user@example.com",
                    ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME,
                    "Home", "", "", "", "", "", "", "",
                    0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.sipAddresses.size)
        assertEquals("sip:user@example.com", rows.sipAddresses.first().address)
        assertEquals(ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME, rows.sipAddresses.first().type)
        assertEquals("Home", rows.sipAddresses.first().label)
    }

    @Test
    fun parseContactRows_event_anniversary() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                    "2020-05-20",
                    ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,
                    "Anniversary", "", "", "", "", "", "", "",
                    0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.events.size)
        assertEquals("2020-05-20", rows.events.first().raw)
        assertEquals(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY, rows.events.first().type)
        assertEquals("Anniversary", rows.events.first().label)
    }

    @Test
    fun parseContactRows_event_birthday() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                    "1990-01-15",
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                    "", "", "", "", "", "", "", "",
                    0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertTrue(rows.events.isEmpty())
        assertEquals("1990-01-15", rows.birthday?.raw)
    }

    @Test
    fun parseContactRows_groupMembership() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    "5", "", "", "", "", "", "", "", "", "",
                    0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.groupMemberships.size)
        assertEquals(5L, rows.groupMemberships.first().groupId)
    }

    @Test
    fun parseContactRows_multipleRows() {
        val cursor = MatrixCursor(CONTACT_DATA_PROJECTION).apply {
            addRow(
                arrayOf<Any?>(
                    1L, 10L, 20L,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    "+1234567890",
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                    "", "", "", "", "", "", "", "",
                    1,
                ),
            )
            addRow(
                arrayOf<Any?>(
                    2L, 10L, 20L,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    "john@example.com",
                    ContactsContract.CommonDataKinds.Email.TYPE_HOME,
                    "", "", "", "", "", "", "", "",
                    0,
                ),
            )
        }
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.phones.size)
        assertEquals(1, rows.emails.size)
    }
}
