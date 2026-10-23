package com.m4isper.contactsapp.domain.usecase.groups

import com.m4isper.contactsapp.domain.repository.GroupsRepository

/** Убирает контакты из группы. */
class RemoveContactsFromGroupUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(
        groupId: Long,
        contactIds: List<Long>,
    ) = repository.removeContactsFromGroup(groupId, contactIds)
}
