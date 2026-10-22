package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsReader
import dev.zacsweers.metro.Inject

@Inject
class GetContactsUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(): List<ContactDetails> = repository.getContacts()
}
