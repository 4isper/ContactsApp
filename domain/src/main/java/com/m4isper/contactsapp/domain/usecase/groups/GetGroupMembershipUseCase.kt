package com.m4isper.contactsapp.domain.usecase.groups

import com.m4isper.contactsapp.domain.repository.GroupsRepository

class GetGroupMembershipUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(): Map<Long, Set<Long>> = repository.getMembership()
}
