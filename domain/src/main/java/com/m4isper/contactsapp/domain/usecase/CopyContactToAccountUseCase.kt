package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

/** Копирует контакт в другой аккаунт: исходный остаётся на месте. */
@Inject
class CopyContactToAccountUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(
        contactId: Long,
        account: ContactAccount,
    ): Long = repository.copyContact(contactId, account)
}
