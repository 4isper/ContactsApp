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
import com.m4isper.contactsapp.data.datasource.write.groups.GroupMergePlan
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Потолок числа id в одном SQL-выражении: большие списки режутся на чанки. */
private const val IDS_PER_QUERY = 500

private const val GROUP_TITLE_SORT_ORDER = "${Groups.TITLE} ASC"

/**
 * Работа с [ContactsContract.Groups] и строками GroupMembership: только запросы
 * к провайдеру и операции над ним, без логики слияния и диспетчеров.
 */
@Suppress("TooManyFunctions") // Осознанно: у провайдера групп есть своя функция на каждую операцию.
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContactsContractGroupsProvider(
    private val context: Context,
) : GroupsProvider {
    override fun loadGroups(): List<ContactGroup> {
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
        return groups
    }

    override fun loadMembership(): Map<Long, Set<Long>> =
        queryMembership(selection = "${Data.MIMETYPE}=?", args = arrayOf(GroupMembership.CONTENT_ITEM_TYPE))

    override fun loadContactGroupIds(contactId: Long): Set<Long> =
        queryMembership(
            selection = "${Data.MIMETYPE}=? AND ${Data.CONTACT_ID}=?",
            args = arrayOf(GroupMembership.CONTENT_ITEM_TYPE, contactId.toString()),
        )[contactId].orEmpty()

    override fun createGroup(
        title: String,
        account: ContactAccount,
    ): Long {
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
        return uri?.let(ContentUris::parseId) ?: error("group insert failed: \"$title\"")
    }

    override fun renameGroup(
        groupId: Long,
        title: String,
    ) {
        context.contentResolver.update(
            ContentUris.withAppendedId(Groups.CONTENT_URI, groupId),
            ContentValues().apply { put(Groups.TITLE, title) },
            null,
            null,
        )
    }

    override fun deleteGroup(groupId: Long) {
        context.contentResolver.delete(
            ContentUris.withAppendedId(Groups.CONTENT_URI, groupId),
            null,
            null,
        )
    }

    override fun addMembers(
        groupId: Long,
        contactIds: List<Long>,
    ) {
        val inserts = membershipInserts(groupId, contactIds)
        if (inserts.isNotEmpty()) {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(inserts))
        }
    }

    override fun removeMembers(
        groupId: Long,
        contactIds: List<Long>,
    ) {
        val ids = contactIds.distinct()
        if (ids.isEmpty()) return
        val targets = rawContactsOf(ids)
        if (targets.isEmpty()) return
        val rawIds = targets.values.toList()
        rawIds.chunked(IDS_PER_QUERY).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val selection =
                "${Data.MIMETYPE}=? AND ${Data.RAW_CONTACT_ID} IN ($placeholders) AND " +
                    "${GroupMembership.GROUP_ROW_ID}=?"
            val args =
                arrayOf(
                    GroupMembership.CONTENT_ITEM_TYPE,
                    *chunk.map(Long::toString).toTypedArray(),
                    groupId.toString(),
                )
            context.contentResolver.delete(Data.CONTENT_URI, selection, args)
        }
    }

    override fun membersOf(groupId: Long): List<Long> {
        val ids = mutableListOf<Long>()
        context.contentResolver
            .query(
                Data.CONTENT_URI,
                arrayOf(Data.CONTACT_ID),
                "${Data.MIMETYPE}=? AND ${GroupMembership.GROUP_ROW_ID}=?",
                arrayOf(GroupMembership.CONTENT_ITEM_TYPE, groupId.toString()),
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Data.CONTACT_ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idIndex)
                }
            }
        return ids.distinct()
    }

    override fun rawContactsOf(contactIds: List<Long>): Map<Long, Long> {
        val ids = contactIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        val targets = mutableMapOf<Long, Long>()
        ids.chunked(IDS_PER_QUERY).forEach { chunk ->
            context.contentResolver
                .query(
                    RawContacts.CONTENT_URI,
                    arrayOf(RawContacts._ID, RawContacts.CONTACT_ID),
                    "${RawContacts.CONTACT_ID} IN (${placeholders(chunk.size)})",
                    chunk.map(Long::toString).toTypedArray(),
                    "${RawContacts._ID} ASC",
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(RawContacts._ID)
                    val contactIndex = cursor.getColumnIndexOrThrow(RawContacts.CONTACT_ID)
                    while (cursor.moveToNext()) {
                        targets.putIfAbsent(cursor.getLong(contactIndex), cursor.getLong(idIndex))
                    }
                }
        }
        return targets
    }

    override fun membersAlreadyIn(
        groupId: Long,
        contactIds: List<Long>,
    ): Set<Long> {
        val ids = contactIds.distinct()
        if (ids.isEmpty()) return emptySet()
        val found = mutableSetOf<Long>()
        ids.chunked(IDS_PER_QUERY).forEach { chunk ->
            context.contentResolver
                .query(
                    Data.CONTENT_URI,
                    arrayOf(Data.CONTACT_ID),
                    "${Data.MIMETYPE}=? AND ${Data.DATA1}=? AND ${Data.CONTACT_ID} IN (${placeholders(chunk.size)})",
                    arrayOf(GroupMembership.CONTENT_ITEM_TYPE, groupId.toString()) +
                        chunk.map(Long::toString).toTypedArray(),
                    null,
                )?.use { cursor ->
                    val contactIndex = cursor.getColumnIndexOrThrow(Data.CONTACT_ID)
                    while (cursor.moveToNext()) {
                        found += cursor.getLong(contactIndex)
                    }
                }
        }
        return found
    }

    /**
     * Один batch: участники переносятся в целевую группу, исходная группа удаляется
     * вместе с ними. Провайдер применяет batch транзакционно — при ошибке не остаётся
     * ни перенесённых членств, ни удалённой группы.
     */
    override fun mergeGroups(plan: GroupMergePlan) {
        val operations =
            ArrayList<ContentProviderOperation>(plan.inserts.size + 1).apply {
                plan.inserts.forEach { insert ->
                    add(
                        ContentProviderOperation
                            .newInsert(Data.CONTENT_URI)
                            .withValue(Data.RAW_CONTACT_ID, insert.rawContactId)
                            .withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                            .withValue(GroupMembership.GROUP_ROW_ID, insert.groupId)
                            .build(),
                    )
                }
                add(
                    ContentProviderOperation
                        .newDelete(ContentUris.withAppendedId(Groups.CONTENT_URI, plan.sourceGroupId))
                        .build(),
                )
            }
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
    }

    /** Вставки членства, которых ещё нет в целевой группе. */
    private fun membershipInserts(
        groupId: Long,
        contactIds: List<Long>,
    ): List<ContentProviderOperation> {
        val ids = contactIds.distinct()
        if (ids.isEmpty()) return emptyList()
        val inserts = mutableListOf<ContentProviderOperation>()
        ids.chunked(IDS_PER_QUERY).forEach { chunk ->
            val targets = rawContactsOf(chunk)
            val alreadyIn = membersAlreadyIn(groupId, chunk)
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
        return inserts
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

    private fun placeholders(count: Int): String = List(count) { "?" }.joinToString(",")
}
