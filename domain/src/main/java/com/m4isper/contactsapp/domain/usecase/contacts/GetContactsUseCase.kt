package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsReader

class GetContactsUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(): List<ContactDetails> = repository.getContacts()
}
