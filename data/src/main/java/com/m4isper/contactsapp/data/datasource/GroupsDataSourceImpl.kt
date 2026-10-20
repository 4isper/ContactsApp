package com.m4isper.contactsapp.data.datasource

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Потолок числа id в одном SQL-выражении: большие списки режутся на чанки. */
private const val IDS_PER_QUERY = 500

private const val GROUP_TITLE_SORT_ORDER = "${Groups.TITLE} ASC"

/**
 * Единственное место работы с [ContactsContract.Groups] и строками GroupMembership.
 * Группа создаётся в выбранном аккаунте (по умолчанию — локально); чтение и членство
 * доступны для групп любых аккаунтов.
 */
@Suppress("TooManyFunctions")
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class GroupsDataSourceImpl(
    private val context: Context,
) : GroupsDataSource {
    override suspend fun loadGroups(): List<ContactGroup> =
        withContext(Dispatchers.IO) {
            val groups = mutableListOf<ContactGroup>()
            context.contentResolver
                .query(
                    Groups.CONTENT_URI,
                    arrayOf(Groups._ID, Groups.TITLE, Groups.ACCOUNT_NAME, Groups.ACCOUNT_TYPE),
                    "${Groups.DELETED}=0 AND " +
                        "${Groups.GROUP_VISIBLE}=1 AND " +
                        "${Groups.TITLE} IS NOT NULL AND ${Groups.TITLE} != ''",
                    null,
                    GROUP_TITLE_SORT_ORDER,
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(Groups._ID)
                    val titleIndex = cursor.getColumnIndexOrThrow(Groups.TITLE)
                    val accountNameIndex = cursor.getColumnIndexOrThrow(Groups.ACCOUNT_NAME)
                    val accountTypeIndex = cursor.getColumnIndexOrThrow(Groups.ACCOUNT_TYPE)
                    while (cursor.moveToNext()) {
                        groups +=
                            ContactGroup(
                                id = cursor.getLong(idIndex),
                                title = cursor.getString(titleIndex).orEmpty(),
                                accountName = cursor.getString(accountNameIndex),
                                accountType = cursor.getString(accountTypeIndex),
                            )
                    }
                }
            groups
        }

    override suspend fun loadMembership(): Map<Long, Set<Long>> =
        withContext(Dispatchers.IO) {
            queryMembership(selection = "${Data.MIMETYPE}=?", args = arrayOf(GroupMembership.CONTENT_ITEM_TYPE))
        }

    override suspend fun loadContactGroupIds(contactId: Long): Set<Long> =
        withContext(Dispatchers.IO) {
            queryMembership(
                selection = "${Data.MIMETYPE}=? AND ${Data.CONTACT_ID}=?",
                args = arrayOf(GroupMembership.CONTENT_ITEM_TYPE, contactId.toString()),
            )[contactId].orEmpty()
        }

    override suspend fun createGroup(title: String): ContactGroup = createGroup(title, ContactAccount.Local)

    override suspend fun createGroup(
        title: String,
        account: ContactAccount,
    ): ContactGroup =
        withContext(Dispatchers.IO) {
            val uri =
                context.contentResolver.insert(
                    Groups.CONTENT_URI,
                    ContentValues().apply {
                        put(Groups.TITLE, title)
                        put(Groups.GROUP_VISIBLE, 1)
                        if (!account.isLocal) {
                            put(Groups.ACCOUNT_NAME, account.name)
                            put(Groups.ACCOUNT_TYPE, account.type)
                        }
                    },
                )
            val id = uri?.let(ContentUris::parseId) ?: error("group insert failed: \"$title\"")
            ContactGroup(
                id = id,
                title = title,
                accountName = account.name,
                accountType = account.type,
            )
        }

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ): Unit =
        withContext(Dispatchers.IO) {
            context.contentResolver.update(
                ContentUris.withAppendedId(Groups.CONTENT_URI, groupId),
                ContentValues().apply { put(Groups.TITLE, title) },
                null,
                null,
            )
        }

    override suspend fun deleteGroup(groupId: Long): Unit =
        withContext(Dispatchers.IO) {
            context.contentResolver.delete(
                ContentUris.withAppendedId(Groups.CONTENT_URI, groupId),
                null,
                null,
            )
        }

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ): Unit =
        withContext(Dispatchers.IO) {
            val ids = contactIds.distinct()
            if (ids.isEmpty()) return@withContext
            val inserts = mutableListOf<ContentProviderOperation>()
            ids.chunked(IDS_PER_QUERY).forEach { chunk ->
                val targets = targetRawContacts(chunk)
                val alreadyIn = contactsHavingGroup(groupId, chunk)
                targets.forEach { (contactId, rawContactId) ->
                    if (contactId !in alreadyIn) {
                        inserts +=
                            ContentProviderOperation
                                .newInsert(Data.CONTENT_URI)
                                .withValue(Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                                .withValue(GroupMembership.GROUP_ROW_ID, groupId)
                                .build()
                    }
                }
            }
            if (inserts.isNotEmpty()) {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(inserts))
            }
        }

    private fun queryMembership(
        selection: String,
        args: Array<String>,
    ): Map<Long, Set<Long>> {
        val membership = mutableMapOf<Long, MutableSet<Long>>()
        context.contentResolver
            .query(
                Data.CONTENT_URI,
                arrayOf(Data.CONTACT_ID, Data.DATA1),
                selection,
                args,
                null,
            )?.use { cursor ->
                val contactIndex = cursor.getColumnIndexOrThrow(Data.CONTACT_ID)
                val groupIndex = cursor.getColumnIndexOrThrow(Data.DATA1)
                while (cursor.moveToNext()) {
                    val groupId = cursor.getLong(groupIndex)
                    if (groupId <= 0) continue
                    membership.getOrPut(cursor.getLong(contactIndex)) { mutableSetOf() } += groupId
                }
            }
        return membership
    }

    /** Целевой raw contact на контакт: минимальный raw id — тот же выбор, что у редактора. */
    private fun targetRawContacts(contactIds: List<Long>): Map<Long, Long> {
        val targets = mutableMapOf<Long, Long>()
        context.contentResolver
            .query(
                RawContacts.CONTENT_URI,
                arrayOf(RawContacts._ID, RawContacts.CONTACT_ID),
                "${RawContacts.CONTACT_ID} IN (${placeholders(contactIds.size)})",
                contactIds.map(Long::toString).toTypedArray(),
                "${RawContacts._ID} ASC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(RawContacts._ID)
                val contactIndex = cursor.getColumnIndexOrThrow(RawContacts.CONTACT_ID)
                while (cursor.moveToNext()) {
                    targets.putIfAbsent(cursor.getLong(contactIndex), cursor.getLong(idIndex))
                }
            }
        return targets
    }

    /** Контакты из списка, у которых эта группа уже есть — в любом из raw contacts. */
    private fun contactsHavingGroup(
        groupId: Long,
        contactIds: List<Long>,
    ): Set<Long> {
        val found = mutableSetOf<Long>()
        context.contentResolver
            .query(
                Data.CONTENT_URI,
                arrayOf(Data.CONTACT_ID),
                "${Data.MIMETYPE}=? AND ${Data.DATA1}=? AND ${Data.CONTACT_ID} IN (${placeholders(contactIds.size)})",
                arrayOf(GroupMembership.CONTENT_ITEM_TYPE, groupId.toString()) +
                    contactIds.map(Long::toString).toTypedArray(),
                null,
            )?.use { cursor ->
                val contactIndex = cursor.getColumnIndexOrThrow(Data.CONTACT_ID)
                while (cursor.moveToNext()) {
                    found += cursor.getLong(contactIndex)
                }
            }
        return found
    }

    private fun placeholders(count: Int): String = List(count) { "?" }.joinToString(",")
}
