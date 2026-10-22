package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsReader

class GetContactDetailsUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(contactId: Long): ContactDetails? = repository.getContactDetails(contactId)
}
