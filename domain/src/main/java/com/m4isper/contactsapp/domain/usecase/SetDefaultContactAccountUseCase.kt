package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.AccountSettings

/** Аккаунт по умолчанию для новых контактов. */
class SetDefaultContactAccountUseCase(
    private val repository: AccountSettings,
) {
    suspend operator fun invoke(account: ContactAccount) = repository.setDefaultContactAccount(account)
}
