package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.GroupsRepository
import dev.zacsweers.metro.Inject

@Inject
class DeleteGroupUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(groupId: Long) = repository.deleteGroup(groupId)
}
