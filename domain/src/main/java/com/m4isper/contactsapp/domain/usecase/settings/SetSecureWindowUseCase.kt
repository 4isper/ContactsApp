package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.PrivacySettings

class SetSecureWindowUseCase(
    private val repository: PrivacySettings,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSecureWindow(enabled)
}
