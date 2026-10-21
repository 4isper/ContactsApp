package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

/** Страница контактов: список грузится по частям, чтобы появляться сразу. */
@Inject
class GetContactsPageUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = repository.getContactsPage(offset, limit)
}
