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

/** Постраничная загрузка контактов: страницы не пересекаются и покрывают список. */
@RunWith(AndroidJUnit4::class)
class ContactsPagingTest {
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val dataSource by lazy { ContactsDataSourceImpl(context) }

    @Test
    fun pages_cover_the_same_list_as_a_full_load() =
        runBlocking {
            val created = (1..5).map { createContact("Paging $it") }
            try {
                awaitIndexed(created)
                val all = dataSource.loadContacts().map { it.contactId }
                val pages =
                    (0 until 50)
                        .map { dataSource.loadContactsPage(offset = it * PAGE_SIZE, limit = PAGE_SIZE) }
                        .flatten()
                        .map { it.contactId }

                assertEquals(all, pages)
                assertEquals(pages.size, pages.distinct().size)
                assertTrue(created.all { it in pages })
            } finally {
                created.forEach { deleteContact(it) }
            }
        }

    @Test
    fun page_beyond_the_end_is_empty() =
        runBlocking {
            assertTrue(dataSource.loadContactsPage(offset = 100_000, limit = 10).isEmpty())
        }

    /** Провайдер индексирует контакты асинхронно: ждём, пока они появятся в выборке. */
    private suspend fun awaitIndexed(contactIds: List<Long>) {
        val deadline = System.currentTimeMillis() + INDEX_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val loaded = dataSource.loadContactsPage(offset = 0, limit = 100).map { it.contactId }
            if (contactIds.all { it in loaded }) return
            kotlinx.coroutines.delay(200)
        }
    }

    private fun createContact(name: String): Long {
        val rawValues =
            ContentValues().apply {
                putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
            }
        val rawUri =
            requireNotNull(
                context.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, rawValues),
            )
        val rawId = requireNotNull(rawUri.lastPathSegment).toLong()
        val nameValues =
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)
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

    private fun deleteContact(contactId: Long) {
        context.contentResolver.delete(
            android.net.Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString()),
            null,
            null,
        )
    }

    private companion object {
        const val INDEX_TIMEOUT_MILLIS = 10_000L
        const val PAGE_SIZE = 2
    }
}
