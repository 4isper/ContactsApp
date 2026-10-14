package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.Contact
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

@Inject
class GetContactsUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(): List<Contact> = repository.getContacts()
}
