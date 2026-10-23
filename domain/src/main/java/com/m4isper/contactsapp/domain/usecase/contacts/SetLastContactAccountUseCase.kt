package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.repository.settings.AccountSettings

class SetLastContactAccountUseCase(
    private val repository: AccountSettings,
) {
    suspend operator fun invoke(account: ContactAccount) = repository.setLastContactAccount(account)
}
