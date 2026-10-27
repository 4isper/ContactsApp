package com.m4isper.contactsapp.domain.usecase.groups

import com.m4isper.contactsapp.domain.repository.GroupsRepository

class DeleteGroupUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(groupId: Long) = repository.deleteGroup(groupId)
}
