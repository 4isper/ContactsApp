package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.repository.ContactsReader

class GetContactFormUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(contactId: Long): ContactForm? = repository.getContactForm(contactId)
}
