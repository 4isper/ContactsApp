package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.repository.ContactsReader

class GetContactFormUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(contactId: Long): ContactForm? = repository.getContactForm(contactId)
}
