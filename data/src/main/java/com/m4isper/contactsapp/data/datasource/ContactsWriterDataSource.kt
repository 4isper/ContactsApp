package com.m4isper.contactsapp.data.datasource

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.domain.model.ContactForm
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val MAX_PHOTO_PX = 720
private const val PHOTO_QUALITY = 85

private val WRITER_DATA_PROJECTION =
    arrayOf(
        ContactsContract.Data._ID,
        ContactsContract.Data.RAW_CONTACT_ID,
        ContactsContract.Data.MIMETYPE,
        ContactsContract.Data.DATA1,
        ContactsContract.Data.DATA2,
        ContactsContract.Data.DATA3,
        ContactsContract.Data.DATA4,
    )

@Inject
@SingleIn(AppScope::class)
class ContactsWriterDataSource(
    private val context: Context,
) {
    suspend fun createContact(form: ContactForm): Unit =
        withContext(Dispatchers.IO) {
            context.contentResolver.applyBatch(
                ContactsContract.AUTHORITY,
                buildCreateOperations(form, photo = preparePhoto(form)),
            )
        }

    suspend fun updateContact(
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

    suspend fun loadContactRows(contactId: Long): ContactRows =
        withContext(Dispatchers.IO) {
            val cursor =
                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    WRITER_DATA_PROJECTION,
                    "${ContactsContract.Data.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    null,
                )
            cursor?.use { parseContactRows(it) } ?: ContactRows(hasNameRow = false)
        }

    suspend fun loadRawContactId(contactId: Long): Long? =
        withContext(Dispatchers.IO) {
            val cursor =
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    "${ContactsContract.RawContacts.CONTACT_ID}=?",
                    arrayOf(contactId.toString()),
                    null,
                )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                } else {
                    null
                }
            }
        }

    suspend fun deleteContact(contactId: Long): Unit =
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
            form.photoUri?.let(Uri::parse)?.let(::readDownscaledPhoto)
        } else {
            null
        }

    private fun readDownscaledPhoto(uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("Unsupported image: $uri")
        }
        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_PX)
            }
        val decoded =
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IllegalArgumentException("Can't read image: $uri")
        val scaled = scaleDown(decoded, MAX_PHOTO_PX)
        return try {
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out)
                out.toByteArray()
            }
        } finally {
            scaled.recycle()
            if (scaled !== decoded) decoded.recycle()
        }
    }
}

private fun parseContactRows(cursor: Cursor): ContactRows {
    val phones = mutableListOf<ExistingPhone>()
    val emails = mutableListOf<ExistingEmail>()
    val addresses = mutableListOf<ExistingAddress>()
    val websites = mutableListOf<ExistingWebsite>()
    var organization: ExistingOrganization? = null
    var note: ExistingNote? = null
    var birthday: ExistingBirthday? = null
    var hasNameRow = false
    val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)
    val rawIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID)
    val mimeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
    val data1Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
    val data2Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
    val data3Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
    val data4Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA4)
    while (cursor.moveToNext()) {
        val dataId = cursor.getLong(idIndex)
        val rawId = cursor.getLong(rawIndex)
        val data1 = cursor.getString(data1Index).orEmpty()
        when (cursor.getString(mimeIndex)) {
            StructuredName.CONTENT_ITEM_TYPE -> hasNameRow = true
            Phone.CONTENT_ITEM_TYPE ->
                phones +=
                    ExistingPhone(dataId, rawId, data1, cursor.getInt(data2Index), cursor.getString(data3Index))
            Email.CONTENT_ITEM_TYPE ->
                emails +=
                    ExistingEmail(dataId, rawId, data1, cursor.getInt(data2Index), cursor.getString(data3Index))
            StructuredPostal.CONTENT_ITEM_TYPE ->
                addresses +=
                    ExistingAddress(dataId, rawId, data1, cursor.getInt(data2Index), cursor.getString(data3Index))
            Website.CONTENT_ITEM_TYPE ->
                websites += ExistingWebsite(dataId, rawId, data1)
            Organization.CONTENT_ITEM_TYPE ->
                if (organization == null) {
                    organization =
                        ExistingOrganization(dataId, rawId, data1, cursor.getString(data4Index).orEmpty())
                }
            Note.CONTENT_ITEM_TYPE ->
                if (note == null) {
                    note = ExistingNote(dataId, rawId, data1)
                }
            Event.CONTENT_ITEM_TYPE ->
                if (cursor.getInt(data2Index) == Event.TYPE_BIRTHDAY && birthday == null) {
                    birthday = ExistingBirthday(dataId, rawId, data1)
                }
        }
    }
    return ContactRows(
        hasNameRow = hasNameRow,
        phones = phones,
        emails = emails,
        addresses = addresses,
        websites = websites,
        organization = organization,
        note = note,
        birthday = birthday,
    )
}

/** Степень двойки, после которой декодированная картинка станет меньше max. */
internal fun sampleSize(
    width: Int,
    height: Int,
    max: Int,
): Int {
    var sample = 1
    while (width / (sample * 2) >= max || height / (sample * 2) >= max) {
        sample *= 2
    }
    return sample
}

private fun scaleDown(
    bitmap: Bitmap,
    max: Int,
): Bitmap {
    val largest = maxOf(bitmap.width, bitmap.height)
    if (largest <= max) return bitmap
    val ratio = max.toFloat() / largest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt().coerceAtLeast(1),
        (bitmap.height * ratio).toInt().coerceAtLeast(1),
        true,
    )
}
