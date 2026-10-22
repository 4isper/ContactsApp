package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.repository.GroupsRepository

class GetContactGroupsUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(contactId: Long): List<ContactGroup> = repository.getContactGroups(contactId)
}
