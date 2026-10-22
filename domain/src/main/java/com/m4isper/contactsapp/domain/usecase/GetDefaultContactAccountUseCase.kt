package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.SettingsReader
import kotlinx.coroutines.flow.first

/** Аккаунт по умолчанию для новых контактов. */
class GetDefaultContactAccountUseCase(
    private val repository: SettingsReader,
) {
    suspend operator fun invoke(): ContactAccount = repository.observeSettings().first().defaultContactAccount
}
