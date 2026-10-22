package com.m4isper.contactsapp.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.data.datasource.ContactsDataSourceImpl
import com.m4isper.contactsapp.data.datasource.ContactsWriterDataSourceImpl
import com.m4isper.contactsapp.data.repository.ContactsRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Фото контакта живёт в провайдере отдельной строкой: проверяем, что приложение
 * умеет записать фото и прочитать его обратно тем же путём, что и экспорт в .vcf.
 */
@RunWith(AndroidJUnit4::class)
class ContactsPhotoTest {
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun repository(): ContactsRepositoryImpl =
        ContactsRepositoryImpl(
            dataSource = ContactsDataSourceImpl(context),
            writer = ContactsWriterDataSourceImpl(context),
        )

    @Test
    fun photo_written_to_the_provider_can_be_read_back() =
        runBlocking {
            val contactId = createContactWithPhoto()
            try {
                val bytes = repository().loadContactPhoto(contactId)
                assertNotNull("фото должно читаться обратно", bytes)
                assertTrue(bytes!!.isNotEmpty())
            } finally {
                deleteContact(contactId)
            }
        }

    @Test
    fun contact_without_photo_has_no_bytes() =
        runBlocking {
            val contactId = createContact()
            try {
                assertNull(repository().loadContactPhoto(contactId))
            } finally {
                deleteContact(contactId)
            }
        }

    /** Контакт с фото: строка photo в провайдере и байты картинки. */
    private fun createContactWithPhoto(): Long {
        val contactId = createContact()
        val rawId = rawIdOf(contactId)
        val photoValues =
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.Photo.PHOTO, tinyJpeg())
            }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, photoValues)
        return contactId
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
        return contactIdOfRaw(requireNotNull(rawUri.lastPathSegment).toLong())
    }

    private fun rawIdOf(contactId: Long): Long {
        val cursor =
            requireNotNull(
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    "${ContactsContract.RawContacts.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    null,
                ),
            )
        return cursor.use {
            check(it.moveToFirst())
            it.getLong(0)
        }
    }

    private fun contactIdOfRaw(rawId: Long): Long {
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
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString()),
            null,
            null,
        )
    }

    /** Минимальная картинка JPEG: одного пикселя достаточно для проверки пути. */
    private fun tinyJpeg(): ByteArray =
        byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0xFF.toByte(),
            0xE0.toByte(),
            0x00,
            0x10,
            0x4A,
            0x46,
            0x49,
            0x46,
            0x00,
            0x01,
            0x01,
            0x00,
            0x00,
            0x01,
            0x00,
            0x01,
            0x00,
            0x00,
            0xFF.toByte(),
            0xDB.toByte(),
            0x00,
            0x43,
            0x00,
            0x08,
            0x06,
            0x06,
            0x07,
            0x06,
            0x05,
            0x08,
            0x07,
            0x07,
            0x07,
            0x09,
            0x09,
            0x08,
            0x0A,
            0x0C,
            0x14,
            0x0D,
            0x0C,
            0x0B,
            0x0B,
            0x0C,
            0x19,
            0x12,
            0x13,
            0x0F,
            0x14,
            0x1D,
            0x1A,
            0x1F,
            0x1E,
            0x1D,
            0x1A,
            0x1C,
            0x1C,
            0x20,
            0x24,
            0x2E,
            0x27,
            0x20,
            0x22,
            0x2C,
            0x23,
            0x1C,
            0x1C,
            0x28,
            0x37,
            0x29,
            0x2C,
            0x30,
            0x31,
            0x34,
            0x34,
            0x34,
            0x1F,
            0x27,
            0x39,
            0x3D,
            0x38,
            0x32,
            0x3C,
            0x2E,
            0x33,
            0x34,
            0x32,
            0xFF.toByte(),
            0xC0.toByte(),
            0x00,
            0x0B,
            0x08,
            0x00,
            0x01,
            0x00,
            0x01,
            0x01,
            0x01,
            0x11,
            0x00,
            0xFF.toByte(),
            0xC4.toByte(),
            0x00,
            0x1F,
            0x00,
            0x00,
            0x01,
            0x05,
            0x01,
            0x01,
            0x01,
            0x01,
            0x01,
            0x01,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x01,
            0x02,
            0x03,
            0x04,
            0x05,
            0x06,
            0x07,
            0x08,
            0x09,
            0x0A,
            0x0B,
            0xFF.toByte(),
            0xC4.toByte(),
            0x00,
            0x14,
            0x10,
            0x01,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0xFF.toByte(),
            0xDA.toByte(),
            0x00,
            0x08,
            0x01,
            0x01,
            0x00,
            0x00,
            0x3F,
            0x00,
            0x37,
            0xFF.toByte(),
            0xD9.toByte(),
        )
}
