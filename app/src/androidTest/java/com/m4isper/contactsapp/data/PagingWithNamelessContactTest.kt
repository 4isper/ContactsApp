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

/**
 * Регрессия: контакт без имени не должен обрывать постраничную загрузку
 * и сдвигать смещение страниц.
 */
@RunWith(AndroidJUnit4::class)
class PagingWithNamelessContactTest {
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val dataSource by lazy { ContactsDataSourceImpl(context) }

    @Test
    fun nameless_contact_does_not_break_the_page_loop() =
        runBlocking {
            val named = (1..3).map { createContact("Paged $it", withName = true) }
            val nameless = createContact("", withName = false)
            awaitIndexed(named.size + 1)

            val loaded = mutableListOf<Long>()
            var offset = 0
            var hasMore = true
            while (hasMore) {
                val page = dataSource.loadContactsPage(offset = offset, limit = PAGE_SIZE)
                loaded += page.map { it.contactId }
                offset += page.size
                hasMore = page.size == PAGE_SIZE
            }

            val all = dataSource.loadContacts().map { it.contactId }
            assertEquals("Список страниц отличается от полной загрузки", all, loaded)
            assertTrue("Контакт с именем потерян", named.all { it in loaded })
            assertTrue("Контакт без имени не должен попадать в список", nameless !in loaded)
        }

    private fun createContact(
        name: String,
        withName: Boolean,
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
        if (withName) {
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
        }
        return contactIdOf(rawId)
    }

    private fun contactIdOf(rawId: Long): Long {
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

    private suspend fun awaitIndexed(expected: Int) {
        val deadline = System.currentTimeMillis() + INDEX_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            if (dataSource.loadContacts().size >= expected) return
            kotlinx.coroutines.delay(300)
        }
    }

    private companion object {
        const val PAGE_SIZE = 2
        const val INDEX_TIMEOUT_MILLIS = 15_000L
    }
}
