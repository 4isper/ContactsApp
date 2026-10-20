package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

/** Аккаунты, доступные для сохранения нового контакта: локальный и синхронизируемые. */
@Inject
class GetContactAccountsUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(): List<ContactAccount> = repository.getContactAccounts()
}
