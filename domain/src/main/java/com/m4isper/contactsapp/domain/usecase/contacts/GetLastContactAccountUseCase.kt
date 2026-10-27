package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.repository.settings.SettingsReader
import kotlinx.coroutines.flow.first

class GetLastContactAccountUseCase(
    private val repository: SettingsReader,
) {
    suspend operator fun invoke(): ContactAccount = repository.observeSettings().first().lastContactAccount
}
