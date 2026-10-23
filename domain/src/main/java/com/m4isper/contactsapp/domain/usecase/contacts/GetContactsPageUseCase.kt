package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsReader

/** Страница контактов: список грузится по частям, чтобы появляться сразу. */
class GetContactsPageUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = repository.getContactsPage(offset, limit)
}
