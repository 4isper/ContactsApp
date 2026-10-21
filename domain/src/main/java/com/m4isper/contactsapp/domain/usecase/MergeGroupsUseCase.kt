package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.GroupsRepository
import dev.zacsweers.metro.Inject

/** Переносит участников одной группы в другую и удаляет исходную группу. */
@Inject
class MergeGroupsUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(
        sourceGroupId: Long,
        targetGroupId: Long,
    ) = repository.mergeGroups(sourceGroupId, targetGroupId)
}
