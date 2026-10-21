package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/** Аккаунт по умолчанию для новых контактов. */
@Inject
class GetDefaultContactAccountUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(): ContactAccount = repository.observeSettings().first().defaultContactAccount
}
