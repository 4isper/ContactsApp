package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsAccounts

class GetContactAccountUseCase(
    private val repository: ContactsAccounts,
) {
    suspend operator fun invoke(contactId: Long): ContactAccount = repository.getContactAccount(contactId)
}
