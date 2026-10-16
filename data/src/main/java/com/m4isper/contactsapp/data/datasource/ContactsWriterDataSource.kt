package com.m4isper.contactsapp.data.datasource

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.PhoneKind
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Существующая строка номера: нужна для diff при обновлении. */
data class ExistingPhone(
    val dataId: Long,
    val rawContactId: Long,
    val number: String,
    val type: Int,
    val label: String?,
)

/** Сырые строки контакта: номера + флаг наличия name-строки. */
data class ContactRows(
    val phones: List<ExistingPhone>,
    val hasNameRow: Boolean,
)

internal fun phoneTypeInt(phone: EditablePhone): Int =
    if (phone.kind == PhoneKind.OTHER && !phone.customLabel.isNullOrBlank()) {
        Phone.TYPE_CUSTOM
    } else {
        when (phone.kind) {
            PhoneKind.MOBILE -> Phone.TYPE_MOBILE
            PhoneKind.HOME -> Phone.TYPE_HOME
            PhoneKind.WORK -> Phone.TYPE_WORK
            PhoneKind.OTHER -> Phone.TYPE_OTHER
        }
    }

private fun phoneInsert(
    rawContactId: Long?,
    rawContactBackRef: Int?,
    phone: EditablePhone,
): ContentProviderOperation.Builder {
    val builder =
        ContentProviderOperation
            .newInsert(ContactsContract.Data.CONTENT_URI)
            .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, phone.number)
            .withValue(Phone.TYPE, phoneTypeInt(phone))
    if (rawContactBackRef != null) {
        builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactBackRef)
    } else {
        builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
    }
    if (phone.kind == PhoneKind.OTHER && !phone.customLabel.isNullOrBlank()) {
        builder.withValue(Phone.LABEL, phone.customLabel)
    }
    return builder
}

/** Операции создания контакта в локальном аккаунте. */
internal fun buildCreateOperations(form: ContactForm): ArrayList<ContentProviderOperation> {
    val ops = ArrayList<ContentProviderOperation>()
    ops +=
        ContentProviderOperation
            .newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
    ops +=
        ContentProviderOperation
            .newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
            .withValue(StructuredName.DISPLAY_NAME, form.name)
            .build()
    form.phones
        .filter { it.number.isNotBlank() }
        .forEach { phone ->
            ops += phoneInsert(rawContactId = null, rawContactBackRef = 0, phone = phone).build()
        }
    return ops
}

/**
 * Операции обновления: имя — update, номера — diff по [ExistingPhone.dataId]:
 * совпавшие — update, новые — insert, пропавшие — delete.
 */
internal fun buildUpdateOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): ArrayList<ContentProviderOperation> {
    val ops = ArrayList<ContentProviderOperation>()
    val existingById = rows.phones.associateBy { it.dataId }

    if (rows.hasNameRow) {
        ops +=
            ContentProviderOperation
                .newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(
                    "${ContactsContract.Data.RAW_CONTACT_ID}=? AND " +
                        "${ContactsContract.Data.MIMETYPE}=?",
                    arrayOf(
                        rawContactId.toString(),
                        StructuredName.CONTENT_ITEM_TYPE,
                    ),
                ).withValue(StructuredName.DISPLAY_NAME, form.name)
                .build()
    } else {
        ops +=
            ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, form.name)
                .build()
    }

    val formIds = form.phones.mapNotNullTo(mutableSetOf()) { it.dataId }
    rows.phones
        .filter { it.dataId !in formIds }
        .forEach { removed ->
            ops +=
                ContentProviderOperation
                    .newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data._ID}=?",
                        arrayOf(removed.dataId.toString()),
                    ).build()
        }

    form.phones
        .filter { it.number.isNotBlank() }
        .forEach { phone ->
            val known = phone.dataId?.let { existingById[it] }
            if (known != null) {
                ops +=
                    ContentProviderOperation
                        .newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data._ID}=?",
                            arrayOf(known.dataId.toString()),
                        ).withValue(Phone.NUMBER, phone.number)
                        .withValue(Phone.TYPE, phoneTypeInt(phone))
                        .build()
            } else {
                ops +=
                    phoneInsert(rawContactId = rawContactId, rawContactBackRef = null, phone = phone)
                        .build()
            }
        }
    return ops
}

@Inject
@SingleIn(AppScope::class)
class ContactsWriterDataSource(
    private val context: Context,
) {
    suspend fun createContact(form: ContactForm): Unit =
        withContext(Dispatchers.IO) {
            context.contentResolver.applyBatch(
                ContactsContract.AUTHORITY,
                buildCreateOperations(form),
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
                buildUpdateOperations(rawContactId, rows, form),
            )
        }

    suspend fun loadContactRows(contactId: Long): ContactRows =
        withContext(Dispatchers.IO) {
            val phones = mutableListOf<ExistingPhone>()
            var hasNameRow = false
            val cursor =
                context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Data._ID,
                        ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE,
                        Phone.NUMBER,
                        Phone.TYPE,
                        Phone.LABEL,
                    ),
                    "${ContactsContract.Data.CONTACT_ID}=? AND " +
                        "${ContactsContract.Data.MIMETYPE} IN (?, ?)",
                    arrayOf(
                        contactId.toString(),
                        Phone.CONTENT_ITEM_TYPE,
                        StructuredName.CONTENT_ITEM_TYPE,
                    ),
                    null,
                )
            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(ContactsContract.Data._ID)
                val rawIndex = it.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID)
                val mimeIndex = it.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
                val numberIndex = it.getColumnIndexOrThrow(Phone.NUMBER)
                val typeIndex = it.getColumnIndexOrThrow(Phone.TYPE)
                val labelIndex = it.getColumnIndexOrThrow(Phone.LABEL)
                while (it.moveToNext()) {
                    if (it.getString(mimeIndex) == StructuredName.CONTENT_ITEM_TYPE) {
                        hasNameRow = true
                    } else {
                        phones +=
                            ExistingPhone(
                                dataId = it.getLong(idIndex),
                                rawContactId = it.getLong(rawIndex),
                                number = it.getString(numberIndex).orEmpty(),
                                type = it.getInt(typeIndex),
                                label = it.getString(labelIndex),
                            )
                    }
                }
            }
            ContactRows(phones = phones, hasNameRow = hasNameRow)
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
}
