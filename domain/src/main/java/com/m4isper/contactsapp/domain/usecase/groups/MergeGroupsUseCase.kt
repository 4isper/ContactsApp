package com.m4isper.contactsapp.domain.usecase.groups

import com.m4isper.contactsapp.domain.repository.GroupsRepository

/** Переносит участников одной группы в другую и удаляет исходную группу. */
class MergeGroupsUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(
        sourceGroupId: Long,
        targetGroupId: Long,
    ) = repository.mergeGroups(sourceGroupId, targetGroupId)
}
