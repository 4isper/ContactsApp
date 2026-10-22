package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import dev.zacsweers.metro.Inject

@Inject
class GetContactAccountUseCase(
    private val repository: ContactsAccounts,
) {
    suspend operator fun invoke(contactId: Long): ContactAccount = repository.getContactAccount(contactId)
}
