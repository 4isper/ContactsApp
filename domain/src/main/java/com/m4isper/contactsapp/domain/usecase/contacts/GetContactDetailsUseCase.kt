package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsReader

class GetContactDetailsUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(contactId: Long): ContactDetails? = repository.getContactDetails(contactId)
}
