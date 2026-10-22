package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.AccountSettings
import dev.zacsweers.metro.Inject

@Inject
class SetLastContactAccountUseCase(
    private val repository: AccountSettings,
) {
    suspend operator fun invoke(account: ContactAccount) = repository.setLastContactAccount(account)
}
