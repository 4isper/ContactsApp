package com.m4isper.contactsapp.data.datasource.read

import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Минимальная реализация Cursor для тестирования parseContactRows без Robolectric. */
private class FakeCursor(
    private val columnNames: Array<String>,
    private val rows: List<List<Any?>>,
) : Cursor {
    private var position = -1

    override fun getCount(): Int = rows.size

    override fun getPosition(): Int = position

    override fun moveToPosition(newPosition: Int): Boolean {
        position = newPosition
        return newPosition in 0 until rows.size
    }

    override fun moveToFirst(): Boolean {
        position = 0
        return rows.isNotEmpty()
    }

    override fun moveToLast(): Boolean {
        position = rows.lastIndex
        return rows.isNotEmpty()
    }

    override fun moveToNext(): Boolean {
        position++
        return position < rows.size
    }

    override fun moveToPrevious(): Boolean {
        position--
        return position >= 0
    }

    override fun isFirst(): Boolean = position == 0

    override fun isLast(): Boolean = position == rows.lastIndex

    override fun isBeforeFirst(): Boolean = position < 0

    override fun isAfterLast(): Boolean = position >= rows.size

    override fun getColumnIndex(columnName: String): Int = columnNames.indexOf(columnName)

    override fun getColumnIndexOrThrow(columnName: String): Int =
        columnNames.indexOf(columnName).takeUnless { it < 0 }
            ?: throw IllegalArgumentException("column '$columnName' not found")

    override fun getColumnNames(): Array<String> = columnNames

    override fun getColumnCount(): Int = columnNames.size

    override fun getString(columnIndex: Int): String = rows[getPosition()][columnIndex]?.toString() ?: ""

    override fun getShort(columnIndex: Int): Short = rows[getPosition()][columnIndex]?.toString()?.toShort() ?: 0

    override fun getInt(columnIndex: Int): Int = rows[getPosition()][columnIndex]?.toString()?.toInt() ?: 0

    override fun getLong(columnIndex: Int): Long = rows[getPosition()][columnIndex]?.toString()?.toLong() ?: 0L

    override fun getFloat(columnIndex: Int): Float = rows[getPosition()][columnIndex]?.toString()?.toFloat() ?: 0f

    override fun getDouble(columnIndex: Int): Double = rows[getPosition()][columnIndex]?.toString()?.toDouble() ?: 0.0

    override fun isNull(columnIndex: Int): Boolean = rows[getPosition()][columnIndex] == null

    override fun getBlob(columnIndex: Int): ByteArray? = null

    override fun getType(columnIndex: Int): Int =
        when (rows[getPosition()][columnIndex]) {
            null -> Cursor.FIELD_TYPE_NULL
            is String -> Cursor.FIELD_TYPE_STRING
            is Int, is Long -> Cursor.FIELD_TYPE_INTEGER
            is Float, is Double -> Cursor.FIELD_TYPE_FLOAT
            else -> Cursor.FIELD_TYPE_STRING
        }

    override fun getColumnName(columnIndex: Int): String = columnNames[columnIndex]

    override fun respond(c: Bundle?): Bundle? = null

    override fun close() = Unit

    override fun deactivate() = Unit

    override fun isClosed(): Boolean = false

    override fun registerContentObserver(observer: ContentObserver?) = Unit

    override fun unregisterContentObserver(observer: ContentObserver?) = Unit

    override fun registerDataSetObserver(observer: DataSetObserver?) = Unit

    override fun unregisterDataSetObserver(observer: DataSetObserver?) = Unit

    override fun setNotificationUri(
        cr: android.content.ContentResolver?,
        uri: Uri?,
    ) = Unit

    override fun getNotificationUri(): Uri? = null

    override fun getWantsAllOnMoveCalls(): Boolean = false

    override fun setExtras(extras: Bundle?) = Unit

    override fun getExtras(): Bundle? = null

    override fun requery(): Boolean = false

    override fun copyStringToBuffer(
        columnIndex: Int,
        buffer: CharArrayBuffer?,
    ) = Unit

    override fun move(offset: Int): Boolean {
        position += offset
        return position in 0 until rows.size
    }
}

private fun cursorOf(vararg values: Any?): Cursor {
    val names =
        arrayOf(
            ContactsContract.Data._ID,
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.DATA4,
            ContactsContract.Data.DATA5,
            ContactsContract.Data.DATA6,
            ContactsContract.Data.DATA7,
            ContactsContract.Data.DATA8,
            ContactsContract.Data.DATA9,
            ContactsContract.Data.DATA10,
            ContactsContract.Data.IS_PRIMARY,
        )
    return FakeCursor(names, listOf(listOf(*values)))
}

class ContactsRowsReaderPureTest {
    @Test
    fun parseContactRows_structuredName() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                "John Doe",
                "John",
                "Doe",
                "Mr.",
                "Middle",
                "Suffix",
                "phoneticGiven",
                "phoneticMiddle",
                "phoneticFamily",
                "",
                0,
            )
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
    fun parseContactRows_address() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                "Lenina 1, Moscow",
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
                "home",
                "Lenina 1",
                "",
                "",
                "Moscow",
                "MO",
                "101000",
                "Russia",
                "",
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.addresses.size)
        assertEquals("Lenina 1", rows.addresses.first().street)
        assertEquals("Moscow", rows.addresses.first().city)
        assertEquals("Russia", rows.addresses.first().country)
        assertEquals(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME, rows.addresses.first().type)
    }

    @Test
    fun parseContactRows_website() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                "https://example.com",
                ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE,
                "homepage",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.websites.size)
        assertEquals("https://example.com", rows.websites.first().url)
        assertEquals(ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE, rows.websites.first().type)
    }

    @Test
    fun parseContactRows_sip() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
                "sip:user@example.com",
                ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME,
                "home",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.sipAddresses.size)
        assertEquals("sip:user@example.com", rows.sipAddresses.first().address)
        assertEquals(ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME, rows.sipAddresses.first().type)
    }

    @Test
    fun parseContactRows_relation() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE,
                "Mother",
                ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER,
                "mom",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.relations.size)
        assertEquals("Mother", rows.relations.first().name)
        assertEquals(ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER, rows.relations.first().type)
    }

    @Test
    fun parseContactRows_event_anniversary() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                "2020-05-20",
                ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.events.size)
        assertEquals("2020-05-20", rows.events.first().raw)
        assertEquals(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY, rows.events.first().type)
    }

    @Test
    fun parseContactRows_event_birthday() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                "1990-01-15",
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertTrue(rows.events.isEmpty())
        assertEquals("1990-01-15", rows.birthday?.raw)
    }

    @Test
    fun parseContactRows_groupMembership() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                "5",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals(1, rows.groupMemberships.size)
        assertEquals(5L, rows.groupMemberships.first().groupId)
    }

    @Test
    fun parseContactRows_organization() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
                "Acme",
                null,
                null,
                "Dev",
                "R&D",
                "coding",
                "HQ",
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertNotNull(rows.organization)
        assertEquals("Acme", rows.organization?.company)
        assertEquals("Dev", rows.organization?.title)
        assertEquals("R&D", rows.organization?.department)
        assertEquals("coding", rows.organization?.jobDescription)
        assertEquals("HQ", rows.organization?.officeLocation)
    }

    @Test
    fun parseContactRows_note() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
                "my note",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals("my note", rows.note?.note)
    }

    @Test
    fun parseContactRows_nickname() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
                "Johnny",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertEquals("Johnny", rows.nickname?.nickname)
    }

    @Test
    fun parseContactRows_unknownMimeIgnored() {
        val cursor =
            cursorOf(
                1L,
                10L,
                20L,
                "vnd.android.cursor.item/custom_vnd",
                "custom data",
                null,
                null,
                null,
                null,
                null,
                null,
                0,
            )
        val rows = parseContactRows(cursor)
        assertTrue(rows.names.isEmpty())
        assertTrue(rows.phones.isEmpty())
    }

    @Test
    fun parseContactRows_emptyCursor() {
        val cursor = FakeCursor(CONTACT_DATA_PROJECTION, emptyList())
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
}
