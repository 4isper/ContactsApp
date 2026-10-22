package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.GroupsRepository

class RenameGroupUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(
        groupId: Long,
        title: String,
    ) = repository.renameGroup(groupId, title)
}
