package com.m4isper.contactsapp.domain.usecase.groups

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.repository.GroupsRepository

class CreateGroupUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(
        title: String,
        account: ContactAccount = ContactAccount.Local,
    ): ContactGroup = repository.createGroup(title, account)
}
