package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.repository.GroupsRepository

class GetGroupsUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(): List<ContactGroup> = repository.getGroups()
}
