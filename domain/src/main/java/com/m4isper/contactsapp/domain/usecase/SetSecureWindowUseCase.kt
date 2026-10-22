package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.PrivacySettings

class SetSecureWindowUseCase(
    private val repository: PrivacySettings,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSecureWindow(enabled)
}
