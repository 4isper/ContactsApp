package com.m4isper.contactsapp.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.data.datasource.FavoritesDataSourceImpl
import com.m4isper.contactsapp.data.repository.FavoritesRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Избранное хранится в провайдере: звезда должна появляться в data-строках
 * и исчезать при снятии — тогда она синхронизируется с системными контактами.
 */
@RunWith(AndroidJUnit4::class)
class FavoritesStarredTest {
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val repository by lazy { FavoritesRepositoryImpl(FavoritesDataSourceImpl(context)) }

    @Test
    fun favorite_is_written_to_the_provider() =
        runBlocking {
            val contactId = createContact()
            try {
                repository.toggleFavorite(contactId)

                assertTrue(starredRawIds().isNotEmpty())
                assertEquals(setOf(contactId), starredContactIds())
            } finally {
                repository.removeFavorite(contactId)
                deleteContact(contactId)
            }
        }

    @Test
    fun removing_favorite_clears_the_provider_row() =
        runBlocking {
            val contactId = createContact()
            try {
                repository.toggleFavorite(contactId)
                repository.removeFavorite(contactId)

                assertTrue(starredRawIds().isEmpty())
            } finally {
                deleteContact(contactId)
            }
        }

    /** id raw-контактов со звездой. */
    private fun starredRawIds(): List<Long> {
        val cursor =
            requireNotNull(
                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
                    "${ContactsContract.Data.MIMETYPE}=?",
                    arrayOf(STARRED_MIME_TYPE),
                    null,
                ),
            )
        return cursor.use {
            if (!it.moveToFirst()) {
                emptyList()
            } else {
                val index = it.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID)
                buildList {
                    do {
                        add(it.getLong(index))
                    } while (it.moveToNext())
                }
            }
        }
    }

    /** id агрегированных контактов со звездой. */
    private fun starredContactIds(): Set<Long> {
        val cursor =
            requireNotNull(
                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data.CONTACT_ID),
                    "${ContactsContract.Data.MIMETYPE}=?",
                    arrayOf(STARRED_MIME_TYPE),
                    null,
                ),
            )
        return cursor.use {
            if (!it.moveToFirst()) {
                emptySet()
            } else {
                val index = it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                buildSet {
                    do {
                        add(it.getLong(index))
                    } while (it.moveToNext())
                }
            }
        }
    }

    private fun createContact(): Long {
        val values =
            ContentValues().apply {
                putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
            }
        val rawUri =
            context.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)
                ?: error("не удалось создать raw contact")
        val rawId = requireNotNull(rawUri.lastPathSegment).toLong()
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
        const val STARRED_MIME_TYPE = "vnd.android.cursor.item/starred"
    }
}
