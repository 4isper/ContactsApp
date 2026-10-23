package com.m4isper.contactsapp.data.datasource.write.groups

import com.m4isper.contactsapp.data.datasource.read.ExistingGroupMembership

/** Что делать со строкой членства в группе при сохранении формы. */
internal sealed interface MembershipWriteOp {
    data class Insert(
        val groupId: Long,
    ) : MembershipWriteOp

    data class Delete(
        val dataId: Long,
    ) : MembershipWriteOp
}

/**
 * План записи членства: выбранная группа без существующей строки вставляется
 * в целевой raw contact, строки снятых групп удаляются — включая дубли,
 * пришедшие из других raw contacts контакта.
 */
internal fun planMembershipWrite(
    existing: List<ExistingGroupMembership>,
    selectedGroupIds: Set<Long>,
): List<MembershipWriteOp> {
    val ops = mutableListOf<MembershipWriteOp>()
    existing.forEach { row ->
        if (row.groupId !in selectedGroupIds) ops += MembershipWriteOp.Delete(row.dataId)
    }
    val existingGroupIds = existing.mapTo(mutableSetOf()) { it.groupId }
    selectedGroupIds.forEach { groupId ->
        if (groupId !in existingGroupIds) ops += MembershipWriteOp.Insert(groupId)
    }
    return ops
}
