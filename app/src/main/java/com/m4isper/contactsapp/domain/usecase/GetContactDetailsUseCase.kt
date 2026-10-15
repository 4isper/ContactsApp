package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

@Inject
class GetContactDetailsUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(contactId: Long): ContactDetails? = repository.getContactDetails(contactId)
}
