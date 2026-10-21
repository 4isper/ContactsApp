package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.GroupsRepository
import dev.zacsweers.metro.Inject

/** Убирает контакты из группы. */
@Inject
class RemoveContactsFromGroupUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(
        groupId: Long,
        contactIds: List<Long>,
    ) = repository.removeContactsFromGroup(groupId, contactIds)
}
