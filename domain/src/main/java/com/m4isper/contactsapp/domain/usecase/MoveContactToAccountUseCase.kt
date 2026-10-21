package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

/** Переносит контакт в другой аккаунт: создаёт копию и удаляет исходный. */
@Inject
class MoveContactToAccountUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(
        contactId: Long,
        account: ContactAccount,
    ): Long = repository.moveContact(contactId, account)
}
