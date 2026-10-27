package com.m4isper.contactsapp.data.datasource

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.m4isper.contactsapp.data.datasource.read.CONTACT_DATA_PROJECTION
import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.data.datasource.read.parseContactRows
import com.m4isper.contactsapp.data.datasource.write.contacts.buildCreateOperations
import com.m4isper.contactsapp.data.datasource.write.contacts.buildUpdateOperations
import com.m4isper.contactsapp.data.datasource.write.contacts.photoWriteOperations
import com.m4isper.contactsapp.data.datasource.write.contacts.readDownscaledPhoto
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContactsWriterDataSourceImpl(
    private val context: Context,
) : ContactsWriterDataSource {
    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long =
        withContext(Dispatchers.IO) {
            val results =
                context.contentResolver.applyBatch(
                    ContactsContract.AUTHORITY,
                    buildCreateOperations(form, photo = preparePhoto(form), account = account),
                )
            val rawUri = results.firstOrNull()?.uri ?: error("raw contact not created")
            contactIdOfRawContact(ContentUris.parseId(rawUri))
        }

    override suspend fun updateContact(
        rawContactId: Long,
        rows: ContactRows,
        form: ContactForm,
    ): Unit =
        withContext(Dispatchers.IO) {
            context.contentResolver.applyBatch(
                ContactsContract.AUTHORITY,
                buildUpdateOperations(rawContactId, rows, form, photo = preparePhoto(form)),
            )
        }

    override suspend fun loadContactRows(contactId: Long): ContactRows =
        withContext(Dispatchers.IO) {
            val cursor =
                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    CONTACT_DATA_PROJECTION,
                    "${ContactsContract.Data.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    null,
                )
            cursor?.use { parseContactRows(it) } ?: ContactRows()
        }

    override suspend fun loadRawContactId(contactId: Long): Long? = loadRawContactIds(contactId).firstOrNull()

    override suspend fun loadRawContactIds(contactId: Long): List<Long> =
        withContext(Dispatchers.IO) {
            val ids = mutableListOf<Long>()
            context.contentResolver
                .query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    "${ContactsContract.RawContacts.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    "${ContactsContract.RawContacts._ID} ASC",
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
                    while (cursor.moveToNext()) {
                        ids += cursor.getLong(idIndex)
                    }
                }
            ids
        }

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ): Unit =
        withContext(Dispatchers.IO) {
            val rawContactId =
                loadRawContactId(contactId) ?: error("no raw contact for id=$contactId")
            context.contentResolver.applyBatch(
                ContactsContract.AUTHORITY,
                ArrayList(photoWriteOperations(rawContactId, photo)),
            )
        }

    override suspend fun linkRawContacts(
        targetRawContactId: Long,
        sourceRawContactIds: List<Long>,
    ): Unit =
        withContext(Dispatchers.IO) {
            val ops = mutableListOf<ContentProviderOperation>()
            sourceRawContactIds.forEach { sourceId ->
                ops +=
                    ContentProviderOperation
                        .newInsert(ContactsContract.AggregationExceptions.CONTENT_URI)
                        .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, targetRawContactId)
                        .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, sourceId)
                        .withValue(
                            ContactsContract.AggregationExceptions.TYPE,
                            ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER,
                        ).build()
            }
            if (ops.isNotEmpty()) {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(ops))
            }
        }

    override suspend fun unlinkRawContacts(
        targetRawContactId: Long,
        sourceRawContactIds: List<Long>,
    ): Unit =
        withContext(Dispatchers.IO) {
            if (sourceRawContactIds.isEmpty()) return@withContext
            val placeholders = sourceRawContactIds.joinToString(",") { "?" }
            val selection =
                "${ContactsContract.AggregationExceptions.RAW_CONTACT_ID1}=? AND " +
                    "${ContactsContract.AggregationExceptions.RAW_CONTACT_ID2} IN ($placeholders) AND " +
                    "${ContactsContract.AggregationExceptions.TYPE}=?"
            val args =
                arrayOf(
                    targetRawContactId.toString(),
                    *sourceRawContactIds.map { it.toString() }.toTypedArray(),
                    ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER.toString(),
                )
            context.contentResolver.delete(ContactsContract.AggregationExceptions.CONTENT_URI, selection, args)
        }

    /** id агрегированного контакта по id его raw contact. */
    private fun contactIdOfRawContact(rawContactId: Long): Long {
        val cursor =
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                "${ContactsContract.RawContacts._ID}=?",
                arrayOf(rawContactId.toString()),
                null,
            )
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID))
            } else {
                error("no contact for raw id=$rawContactId")
            }
        } ?: error("no contact for raw id=$rawContactId")
    }

    override suspend fun deleteContact(contactId: Long): Unit =
        withContext(Dispatchers.IO) {
            val rawIds = mutableSetOf<Long>()
            val cursor =
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    "${ContactsContract.RawContacts.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    null,
                )
            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
                while (it.moveToNext()) {
                    rawIds += it.getLong(idIndex)
                }
            }
            rawIds.forEach { rawId ->
                context.contentResolver.delete(
                    ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawId),
                    null,
                    null,
                )
            }
        }

    /** Байты фото для записи: null, если фото не менялось или его нужно только удалить. */
    private fun preparePhoto(form: ContactForm): ByteArray? =
        if (form.photoChanged) {
            form.photo
                ?.uri
                ?.let(Uri::parse)
                ?.let { readDownscaledPhoto(context.contentResolver, it) }
        } else {
            null
        }
}
