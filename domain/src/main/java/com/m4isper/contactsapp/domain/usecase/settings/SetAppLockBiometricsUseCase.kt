package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.PrivacySettings

/** Разблокировка по биометрии при включённой PIN-защите. */
class SetAppLockBiometricsUseCase(
    private val privacySettings: PrivacySettings,
) {
    suspend operator fun invoke(enabled: Boolean) = privacySettings.setAppLockBiometrics(enabled)
}
