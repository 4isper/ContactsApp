package com.m4isper.contactsapp.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.data.datasource.ContactsDataSourceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Чтение дат контакта: дни рождения и памятные даты из провайдера. */
@RunWith(AndroidJUnit4::class)
class ContactEventsReadingTest {
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val dataSource by lazy { ContactsDataSourceImpl(context) }

    @Test
    fun event_rows_are_read_from_the_provider() =
        runBlocking {
            val contactId = createContactWithEvent("Event reader", "--12-25", type = 0)

            val details = dataSource.loadContact(contactId)

            assertTrue("Ожидалось событие в карточке", details!!.events.isNotEmpty())
            assertEquals(
                25,
                details.events
                    .first()
                    .date.day,
            )
            assertEquals(
                12,
                details.events
                    .first()
                    .date.month,
            )
        }

    private fun createContactWithEvent(
        name: String,
        date: String,
        type: Int,
    ): Long {
        val rawUri =
            requireNotNull(
                context.contentResolver.insert(
                    ContactsContract.RawContacts.CONTENT_URI,
                    ContentValues().apply {
                        putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                        putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
                    },
                ),
            )
        val rawId = requireNotNull(rawUri.lastPathSegment).toLong()
        context.contentResolver.insert(
            ContactsContract.Data.CONTENT_URI,
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                )
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            },
        )
        context.contentResolver.insert(
            ContactsContract.Data.CONTENT_URI,
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                )
                put(ContactsContract.CommonDataKinds.Event.START_DATE, date)
                put(ContactsContract.CommonDataKinds.Event.TYPE, type)
            },
        )
        val cursor =
            requireNotNull(
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                    "${ContactsContract.RawContacts._ID}=?",
                    arrayOf(rawId.toString()),
                    null,
                ),
            )
        return cursor.use {
            check(it.moveToFirst())
            it.getLong(0)
        }
    }
}
