package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.PrivacySettings
import com.m4isper.contactsapp.domain.security.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Защита приложения: вкл/выкл и PIN-код (в настройки попадает только хэш). */
class SetAppLockUseCase(
    private val privacySettings: PrivacySettings,
    private val pinHasher: PinHasher,
) {
    suspend operator fun invoke(
        enabled: Boolean,
        pin: String,
    ) = withContext(Dispatchers.Default) {
        val hash = if (enabled && pin.isNotBlank()) pinHasher.hash(pin) else ""
        privacySettings.setAppLock(enabled, hash)
    }
}
