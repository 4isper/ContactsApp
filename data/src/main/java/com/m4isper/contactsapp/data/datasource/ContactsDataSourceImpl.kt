package com.m4isper.contactsapp.data.datasource

import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.data.datasource.read.CONTACT_DATA_PROJECTION
import com.m4isper.contactsapp.data.datasource.read.ContactHeaderRow
import com.m4isper.contactsapp.data.datasource.read.DataColumns
import com.m4isper.contactsapp.data.datasource.read.DataRow
import com.m4isper.contactsapp.data.datasource.read.aggregateContacts
import com.m4isper.contactsapp.data.datasource.read.buildContactDetails
import com.m4isper.contactsapp.domain.model.ContactDetails
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
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

private const val CONTACT_SORT_ORDER =
    "${ContactsContract.Contacts.DISPLAY_NAME} ASC"

/**
 * Единственное место чтения [ContactsContract].
 * Список читается по таблице Contacts (видны и контакты без номера),
 * детали — по data-строкам контакта.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContactsDataSourceImpl(
    private val context: Context,
) : ContactsDataSource {
    override suspend fun loadContacts(): List<ContactDetails> =
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

    override suspend fun loadContact(contactId: Long): ContactDetails? =
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
                CONTACT_DATA_PROJECTION,
                selection,
                args,
                null,
            )
        cursor?.use {
            val columns = DataColumns(it)
            while (it.moveToNext()) {
                rows +=
                    DataRow(
                        contactId = it.getLong(columns.contactId),
                        mimetype = it.getString(columns.mime).orEmpty(),
                        data1 = it.getString(columns.data1),
                        data2 = it.getString(columns.data2),
                        data3 = it.getString(columns.data3),
                        data4 = it.getString(columns.data4),
                        data5 = it.getString(columns.data5),
                        data6 = it.getString(columns.data6),
                        data7 = it.getString(columns.data7),
                        data8 = it.getString(columns.data8),
                        data9 = it.getString(columns.data9),
                    )
            }
        }
        return rows
    }
}
