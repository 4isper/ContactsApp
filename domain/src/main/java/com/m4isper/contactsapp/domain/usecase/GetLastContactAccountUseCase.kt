package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.SettingsReader
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

@Inject
class GetLastContactAccountUseCase(
    private val repository: SettingsReader,
) {
    suspend operator fun invoke(): ContactAccount = repository.observeSettings().first().lastContactAccount
}
