package com.m4isper.contactsapp.data.datasource.write.groups

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import com.m4isper.contactsapp.data.datasource.read.ExistingGroupMembership
import com.m4isper.contactsapp.data.datasource.write.contacts.dataInsert
import com.m4isper.contactsapp.data.datasource.write.contacts.deleteDataOperation

internal fun groupMembershipValues(groupId: Long): ContentValues =
    ContentValues().apply {
        put(GroupMembership.GROUP_ROW_ID, groupId)
    }

/** Операции членства в группах: diff выбранных групп против существующих строк. */
internal fun membershipOperations(
    rawContactId: Long,
    existing: List<ExistingGroupMembership>,
    selectedGroupIds: Set<Long>,
): List<ContentProviderOperation> =
    planMembershipWrite(existing, selectedGroupIds).map { op ->
        when (op) {
            is MembershipWriteOp.Insert ->
                dataInsert(GroupMembership.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                    .withValues(groupMembershipValues(op.groupId))
                    .build()
            is MembershipWriteOp.Delete -> deleteDataOperation(op.dataId)
        }
    }
