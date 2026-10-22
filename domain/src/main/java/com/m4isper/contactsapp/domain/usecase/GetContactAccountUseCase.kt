package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsAccounts

class GetContactAccountUseCase(
    private val repository: ContactsAccounts,
) {
    suspend operator fun invoke(contactId: Long): ContactAccount = repository.getContactAccount(contactId)
}
