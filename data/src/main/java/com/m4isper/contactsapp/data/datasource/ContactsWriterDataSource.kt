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
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
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
        ContactsContract.Data.DATA5,
        ContactsContract.Data.DATA6,
        ContactsContract.Data.DATA7,
        ContactsContract.Data.DATA8,
        ContactsContract.Data.DATA9,
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
            cursor?.use { parseContactRows(it) } ?: ContactRows()
        }

    suspend fun loadRawContactId(contactId: Long): Long? =
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

/** Индексы колонок проекции WRITER_DATA_PROJECTION. */
private class DataColumns(
    cursor: Cursor,
) {
    val id: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)
    val raw: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID)
    val mime: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
    val data1: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
    val data2: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
    val data3: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
    val data4: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA4)
    val data5: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA5)
    val data6: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA6)
    val data7: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA7)
    val data8: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA8)
    val data9: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA9)
}

/** Накопитель data-строк: одна строка на элемент контакта. */
private class ContactRowsAccumulator {
    private val names = mutableListOf<ExistingName>()
    private val phones = mutableListOf<ExistingPhone>()
    private val emails = mutableListOf<ExistingEmail>()
    private val addresses = mutableListOf<ExistingAddress>()
    private val websites = mutableListOf<ExistingWebsite>()
    private val instantMessages = mutableListOf<ExistingInstantMessage>()
    private val relations = mutableListOf<ExistingRelation>()
    private val events = mutableListOf<ExistingEvent>()
    private var organization: ExistingOrganization? = null
    private var note: ExistingNote? = null
    private var birthday: ExistingBirthday? = null
    private var nickname: ExistingNickname? = null

    fun addRow(
        cursor: Cursor,
        columns: DataColumns,
    ) {
        val dataId = cursor.getLong(columns.id)
        val rawId = cursor.getLong(columns.raw)
        val data1 = cursor.getString(columns.data1).orEmpty()
        when (cursor.getString(columns.mime)) {
            StructuredName.CONTENT_ITEM_TYPE ->
                names += existingName(cursor, columns, dataId, rawId)
            Phone.CONTENT_ITEM_TYPE ->
                phones +=
                    ExistingPhone(dataId, rawId, data1, cursor.getInt(columns.data2), cursor.getString(columns.data3))
            Email.CONTENT_ITEM_TYPE ->
                emails +=
                    ExistingEmail(dataId, rawId, data1, cursor.getInt(columns.data2), cursor.getString(columns.data3))
            StructuredPostal.CONTENT_ITEM_TYPE ->
                addresses +=
                    ExistingAddress(dataId, rawId, data1, cursor.getInt(columns.data2), cursor.getString(columns.data3))
            Website.CONTENT_ITEM_TYPE ->
                websites += ExistingWebsite(dataId, rawId, data1)
            Im.CONTENT_ITEM_TYPE ->
                instantMessages +=
                    ExistingInstantMessage(
                        dataId = dataId,
                        rawContactId = rawId,
                        address = data1,
                        type = cursor.getInt(columns.data2),
                        protocol = cursor.getString(columns.data5)?.toIntOrNull(),
                        customProtocol = cursor.getString(columns.data6),
                    )
            Relation.CONTENT_ITEM_TYPE ->
                relations +=
                    ExistingRelation(
                        dataId = dataId,
                        rawContactId = rawId,
                        name = data1,
                        type = cursor.getInt(columns.data2),
                        label = cursor.getString(columns.data3),
                    )
            Event.CONTENT_ITEM_TYPE -> addEventRow(cursor, columns, dataId, rawId, data1)
            else -> addSingleRow(cursor, columns, dataId, rawId, data1)
        }
    }

    /** Одиночные поля: организация, заметка и никнейм — храним первую строку. */
    private fun addSingleRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawContactId: Long,
        data1: String,
    ) {
        when (cursor.getString(columns.mime)) {
            Organization.CONTENT_ITEM_TYPE ->
                if (organization == null) organization = existingOrganization(cursor, columns, dataId, rawContactId)
            Note.CONTENT_ITEM_TYPE ->
                if (note == null) note = ExistingNote(dataId, rawContactId, data1)
            Nickname.CONTENT_ITEM_TYPE ->
                if (nickname == null) nickname = ExistingNickname(dataId, rawContactId, data1)
        }
    }

    fun toRows(): ContactRows =
        ContactRows(
            names = names,
            phones = phones,
            emails = emails,
            addresses = addresses,
            websites = websites,
            instantMessages = instantMessages,
            relations = relations,
            events = events,
            organization = organization,
            note = note,
            birthday = birthday,
            nickname = nickname,
        )

    /** День рождения хранится тем же mimetype Event, что и прочие даты. */
    private fun addEventRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawContactId: Long,
        data1: String,
    ) {
        val type = cursor.getInt(columns.data2)
        if (type == Event.TYPE_BIRTHDAY) {
            if (birthday == null) birthday = ExistingBirthday(dataId, rawContactId, data1)
        } else {
            events += ExistingEvent(dataId, rawContactId, data1, type, cursor.getString(columns.data3))
        }
    }
}

private fun parseContactRows(cursor: Cursor): ContactRows {
    val columns = DataColumns(cursor)
    val accumulator = ContactRowsAccumulator()
    while (cursor.moveToNext()) {
        accumulator.addRow(cursor, columns)
    }
    return accumulator.toRows()
}

private fun existingName(
    cursor: Cursor,
    columns: DataColumns,
    dataId: Long,
    rawContactId: Long,
): ExistingName =
    ExistingName(
        dataId = dataId,
        rawContactId = rawContactId,
        display = cursor.getString(columns.data1).orEmpty(),
        given = cursor.getString(columns.data2).orEmpty(),
        family = cursor.getString(columns.data3).orEmpty(),
        prefix = cursor.getString(columns.data4).orEmpty(),
        middle = cursor.getString(columns.data5).orEmpty(),
        suffix = cursor.getString(columns.data6).orEmpty(),
        phoneticGiven = cursor.getString(columns.data7).orEmpty(),
        phoneticMiddle = cursor.getString(columns.data8).orEmpty(),
        phoneticFamily = cursor.getString(columns.data9).orEmpty(),
    )

private fun existingOrganization(
    cursor: Cursor,
    columns: DataColumns,
    dataId: Long,
    rawContactId: Long,
): ExistingOrganization =
    ExistingOrganization(
        dataId = dataId,
        rawContactId = rawContactId,
        company = cursor.getString(columns.data1).orEmpty(),
        title = cursor.getString(columns.data4).orEmpty(),
    )

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
