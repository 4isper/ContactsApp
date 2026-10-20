package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import dev.zacsweers.metro.Inject

@Inject
class CreateGroupUseCase(
    private val repository: GroupsRepository,
) {
    suspend operator fun invoke(
        title: String,
        account: ContactAccount = ContactAccount.Local,
    ): ContactGroup = repository.createGroup(title, account)
}
