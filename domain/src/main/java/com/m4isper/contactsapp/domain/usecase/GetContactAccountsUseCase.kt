package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsAccounts

/** Аккаунты, доступные для сохранения нового контакта: локальный и синхронизируемые. */
class GetContactAccountsUseCase(
    private val repository: ContactsAccounts,
) {
    suspend operator fun invoke(): List<ContactAccount> = repository.getContactAccounts()
}
