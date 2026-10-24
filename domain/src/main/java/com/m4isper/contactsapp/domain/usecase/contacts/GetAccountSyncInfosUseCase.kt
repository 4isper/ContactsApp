package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.repository.ContactsAccounts

/** Статус синхронизации аккаунтов для индикатора в UI. */
class GetAccountSyncInfosUseCase(
    private val repository: ContactsAccounts,
) {
    suspend operator fun invoke(): List<AccountSyncInfo> = repository.getAccountSyncInfos()
}
