package com.m4isper.contactsapp.data.datasource

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.m4isper.contactsapp.data.datasource.read.CONTACT_DATA_PROJECTION
import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.data.datasource.read.parseContactRows
import com.m4isper.contactsapp.data.datasource.write.buildCreateOperations
import com.m4isper.contactsapp.data.datasource.write.buildUpdateOperations
import com.m4isper.contactsapp.data.datasource.write.readDownscaledPhoto
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactForm
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
    ): Unit =
        withContext(Dispatchers.IO) {
            context.contentResolver.applyBatch(
                ContactsContract.AUTHORITY,
                buildCreateOperations(form, photo = preparePhoto(form), account = account),
            )
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

    override suspend fun loadRawContactId(contactId: Long): Long? =
        withContext(Dispatchers.IO) {
            val cursor =
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    "${ContactsContract.RawContacts.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    "${ContactsContract.RawContacts._ID} ASC",
                )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                } else {
                    null
                }
            }
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
            form.photoUri?.let(Uri::parse)?.let { readDownscaledPhoto(context.contentResolver, it) }
        } else {
            null
        }
}
