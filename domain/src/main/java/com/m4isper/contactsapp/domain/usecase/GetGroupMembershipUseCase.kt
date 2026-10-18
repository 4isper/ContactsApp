package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.GroupsRepository
import dev.zacsweers.metro.Inject

@Inject
class GetGroupMembershipUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(): Map<Long, Set<Long>> = repository.getMembership()
}
