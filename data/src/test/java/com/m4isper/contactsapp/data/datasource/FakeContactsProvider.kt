package com.m4isper.contactsapp.data.datasource

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID

/**
 * Провайдер контактов в памяти: группы, raw-контакты и строки данных. Нужен,
 * чтобы проверить источники данных без телефона и без настоящего провайдера.
 */
internal class FakeContactsProvider : ContentProvider() {
    /** contactId → id raw-контакта. */
    val rawContacts = mutableMapOf<Long, Long>()

    /** Строки данных провайдера: mime-тип, raw-контакт, контакт и группа. */
    val dataRows = mutableListOf<DataRow>()

    /** Группы контактов: id и название. */
    val groups = mutableMapOf<Long, GroupRow>()

    data class DataRow(
        val id: Long,
        val mimeType: String,
        val rawContactId: Long,
        val contactId: Long,
        val groupId: Long = 0L,
    )

    data class GroupRow(
        val id: Long,
        val title: String,
    )

    /** Условие запроса: колонка и её значения. */
    private data class Filter(
        val column: String,
        val values: List<String>,
    )

    override fun onCreate(): Boolean = true

    /** Таблица провайдера по адресу: группы, raw-контакты или строки данных. */
    private fun tableOf(uri: Uri): String {
        val path = uri.path.orEmpty()
        return when {
            path.contains("groups") -> "groups"
            path.contains("raw_contacts") -> "raw_contacts"
            path.contains("data") -> "data"
            else -> ""
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val columns = projection?.toList() ?: defaultColumns(uri)
        val cursor = MatrixCursor(columns.toTypedArray())
        val filters = parseSelection(selection, selectionArgs)
        matchingRows(uri, filters).forEach { row -> cursor.addRow(columns.map { row[it] }) }
        return cursor
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri {
        if (tableOf(uri) == "groups") {
            val id = groups.size + 1L
            val title = values?.getAsString(ContactsContract.Groups.TITLE).orEmpty()
            groups[id] = GroupRow(id = id, title = title)
            return ContentUris.withAppendedId(uri, id)
        }
        val rawContactId = values?.getAsLong(ContactsContract.Data.RAW_CONTACT_ID) ?: 0L
        val mimeType = values?.getAsString(ContactsContract.Data.MIMETYPE).orEmpty()
        val groupId = values?.getAsLong(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID) ?: 0L
        val contactId = rawContacts.entries.firstOrNull { it.value == rawContactId }?.key ?: 0L
        val row =
            DataRow(
                id = dataRows.size + 1L,
                mimeType = mimeType,
                rawContactId = rawContactId,
                contactId = contactId,
                groupId = groupId,
            )
        dataRows += row
        return ContentUris.withAppendedId(uri, row.id)
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        if (tableOf(uri) == "groups") {
            val id = uri.lastPathSegment?.toLongOrNull()
            val removed =
                if (id == null) {
                    groups.keys.toList()
                } else {
                    listOfNotNull(id.takeIf { groups.containsKey(it) })
                }
            removed.forEach { groupId ->
                groups -= groupId
                // Настоящий провайдер удаляет и членства группы.
                dataRows.removeAll { it.groupId == groupId }
            }
            return removed.size
        }
        val filters = parseSelection(selection, selectionArgs)
        val removed = dataRows.filter { matches(it, filters) }
        removed.forEach { dataRows -= it }
        return removed.size
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val title = values?.getAsString(ContactsContract.Groups.TITLE) ?: return 0
        groups.replaceAll { _, group -> group.copy(title = title) }
        return groups.size
    }

    override fun getType(uri: Uri): String? = null

    /** Batch применяется операциями: так же, как это делает настоящий провайдер. */
    @Throws(android.content.OperationApplicationException::class)
    override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult> {
        val results = arrayOfNulls<ContentProviderResult>(operations.size)
        operations.forEachIndexed { index, operation -> operation.apply(this, results, index) }
        return results.map { it ?: ContentProviderResult(0) }.toTypedArray()
    }

    /** Строки провайдера, подходящие под условие запроса. */
    private fun matchingRows(
        uri: Uri,
        filters: List<Filter>,
    ): List<Map<String, Any?>> =
        when (tableOf(uri)) {
            "raw_contacts" ->
                rawContacts.map { (contactId, rawId) ->
                    mapOf(
                        ContactsContract.RawContacts._ID to rawId,
                        ContactsContract.RawContacts.CONTACT_ID to contactId,
                    )
                }
            "data" ->
                dataRows.map { row ->
                    mapOf(
                        ContactsContract.Data._ID to row.id,
                        ContactsContract.Data.MIMETYPE to row.mimeType,
                        ContactsContract.Data.RAW_CONTACT_ID to row.rawContactId,
                        ContactsContract.Data.CONTACT_ID to row.contactId,
                        ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID to row.groupId,
                    )
                }
            "groups" ->
                groups.values.map { group ->
                    mapOf(
                        ContactsContract.Groups._ID to group.id,
                        ContactsContract.Groups.TITLE to group.title,
                    )
                }
            else -> emptyList()
        }.filter { row -> filters.all { filter -> filter.values.contains(row[filter.column]?.toString()) } }

    private fun matches(
        row: DataRow,
        filters: List<Filter>,
    ): Boolean =
        filters.all { filter ->
            when (filter.column) {
                ContactsContract.Data.MIMETYPE -> filter.values.contains(row.mimeType)
                ContactsContract.Data.RAW_CONTACT_ID -> filter.values.contains(row.rawContactId.toString())
                ContactsContract.Data.CONTACT_ID -> filter.values.contains(row.contactId.toString())
                GROUP_ROW_ID -> filter.values.contains(row.groupId.toString())
                else -> false
            }
        }

    private fun defaultColumns(uri: Uri): List<String> =
        when (tableOf(uri)) {
            "raw_contacts" -> listOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.CONTACT_ID)
            "groups" -> listOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE)
            else -> listOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE)
        }

    /** Условие запроса вида "колонка=?" или "колонка IN (?,?)" и его аргументы. */
    private fun parseSelection(
        selection: String?,
        selectionArgs: Array<out String>?,
    ): List<Filter> {
        if (selection.isNullOrBlank()) return emptyList()
        val args = selectionArgs.orEmpty().toList()
        var index = 0
        return selection.split(" AND ").mapNotNull { part ->
            val column = part.substringBefore('=').substringBefore(" IN").trim()
            val count = part.count { it == '?' }
            if (count == 0) return@mapNotNull null
            val values = args.drop(index).take(count)
            index += count
            Filter(column = column, values = values)
        }
    }
}
