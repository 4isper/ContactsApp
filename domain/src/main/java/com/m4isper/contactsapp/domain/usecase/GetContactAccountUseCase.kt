package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

@Inject
class GetContactAccountUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(contactId: Long): ContactAccount = repository.getContactAccount(contactId)
}
