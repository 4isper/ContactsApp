package com.m4isper.contactsapp.data.datasource.write

/** Вставка членства: raw-контакт вступает в группу. */
data class GroupMembershipInsert(
    val rawContactId: Long,
    val groupId: Long,
)

/**
 * План слияния групп: участники переносятся в целевую группу, исходная группа удаляется.
 * План выполняется одним batch провайдера: либо применяется целиком, либо откатывается.
 */
data class GroupMergePlan(
    val sourceGroupId: Long,
    val targetGroupId: Long,
    val inserts: List<GroupMembershipInsert>,
)

/**
 * План слияния по данным провайдера: в целевую группу попадают только те участники,
 * которых в ней ещё нет; у контактов без raw-строки членство не создаётся.
 */
fun planGroupMerge(
    sourceGroupId: Long,
    targetGroupId: Long,
    members: List<Long>,
    rawContacts: Map<Long, Long>,
    alreadyInTarget: Set<Long>,
): GroupMergePlan {
    val inserts =
        members
            .distinct()
            .filterNot { it in alreadyInTarget }
            .mapNotNull { contactId ->
                rawContacts[contactId]?.let { rawContactId ->
                    GroupMembershipInsert(
                        rawContactId = rawContactId,
                        groupId = targetGroupId,
                    )
                }
            }
    return GroupMergePlan(
        sourceGroupId = sourceGroupId,
        targetGroupId = targetGroupId,
        inserts = inserts,
    )
}
