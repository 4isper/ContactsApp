package com.m4isper.contactsapp.data.datasource

import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.domain.model.ContactDetails
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CONTACT_PROJECTION =
    arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
    )

/**
 * Общая проекция для строк таблицы Data: data1..data9 — физические колонки,
 * в которых лежат все нужные типы (телефон/email — значение в data1,
 * тип в data2, метка в data3; организация — company в data1, title в data4;
 * StructuredName — display в data1, given в data2, family в data3,
 * prefix в data4, middle в data5, suffix в data6,
 * фонетика — given в data7, middle в data8, family в data9).
 */
private val DATA_PROJECTION =
    arrayOf(
        ContactsContract.Data.CONTACT_ID,
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

private const val CONTACT_SORT_ORDER =
    "${ContactsContract.Contacts.DISPLAY_NAME} ASC"

/**
 * Единственное место чтения [ContactsContract].
 * Список читается по таблице Contacts (видны и контакты без номера),
 * детали — по data-строкам контакта.
 */
@Inject
@SingleIn(AppScope::class)
class ContactsDataSource(
    private val context: Context,
) {
    suspend fun loadContacts(): List<ContactDetails> =
        withContext(Dispatchers.IO) {
            val headers = queryHeaders(selection = null, args = null)
            val visibleIds = headers.mapTo(mutableSetOf()) { it.contactId }
            val rows =
                queryData(
                    selection = "${ContactsContract.Data.MIMETYPE} IN (?, ?, ?, ?, ?)",
                    args =
                        arrayOf(
                            Phone.CONTENT_ITEM_TYPE,
                            Email.CONTENT_ITEM_TYPE,
                            StructuredName.CONTENT_ITEM_TYPE,
                            Nickname.CONTENT_ITEM_TYPE,
                            Relation.CONTENT_ITEM_TYPE,
                        ),
                ).filter { it.contactId in visibleIds }
            aggregateContacts(headers, rows)
        }

    suspend fun loadContact(contactId: Long): ContactDetails? =
        withContext(Dispatchers.IO) {
            val header =
                queryHeaders(
                    selection = "${ContactsContract.Contacts._ID} = ?",
                    args = arrayOf(contactId.toString()),
                ).firstOrNull() ?: return@withContext null
            val rows =
                queryData(
                    selection = "${ContactsContract.Data.CONTACT_ID} = ?",
                    args = arrayOf(contactId.toString()),
                )
            buildContactDetails(header, rows)
        }

    private fun queryHeaders(
        selection: String?,
        args: Array<String>?,
    ): List<ContactHeaderRow> {
        val headers = mutableListOf<ContactHeaderRow>()
        val cursor =
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACT_PROJECTION,
                selection,
                args,
                CONTACT_SORT_ORDER,
            )
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            while (it.moveToNext()) {
                headers +=
                    ContactHeaderRow(
                        contactId = it.getLong(idIndex),
                        name = it.getString(nameIndex)?.trim().orEmpty(),
                        photoUri = it.getString(photoIndex),
                    )
            }
        }
        return headers
    }

    private fun queryData(
        selection: String?,
        args: Array<String>?,
    ): List<DataRow> {
        val rows = mutableListOf<DataRow>()
        val cursor =
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                DATA_PROJECTION,
                selection,
                args,
                null,
            )
        cursor?.use {
            val contactIndex = it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeIndex = it.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val data3Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
            val data4Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA4)
            val data5Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA5)
            val data6Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA6)
            val data7Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA7)
            val data8Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA8)
            val data9Index = it.getColumnIndexOrThrow(ContactsContract.Data.DATA9)
            while (it.moveToNext()) {
                rows +=
                    DataRow(
                        contactId = it.getLong(contactIndex),
                        mimetype = it.getString(mimeIndex).orEmpty(),
                        data1 = it.getString(data1Index),
                        data2 = it.getString(data2Index),
                        data3 = it.getString(data3Index),
                        data4 = it.getString(data4Index),
                        data5 = it.getString(data5Index),
                        data6 = it.getString(data6Index),
                        data7 = it.getString(data7Index),
                        data8 = it.getString(data8Index),
                        data9 = it.getString(data9Index),
                    )
            }
        }
        return rows
    }
}
