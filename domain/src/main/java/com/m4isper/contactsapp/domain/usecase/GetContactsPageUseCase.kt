package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsReader
import dev.zacsweers.metro.Inject

/** Страница контактов: список грузится по частям, чтобы появляться сразу. */
@Inject
class GetContactsPageUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = repository.getContactsPage(offset, limit)
}
