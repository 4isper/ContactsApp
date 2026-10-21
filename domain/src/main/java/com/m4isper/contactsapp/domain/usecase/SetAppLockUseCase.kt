package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.hashAppLockPin
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject

/** Защита приложения: вкл/выкл и PIN-код (в настройки попадает только хэш). */
@Inject
class SetAppLockUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(
        enabled: Boolean,
        pin: String,
    ) = repository.setAppLock(enabled, hashAppLockPin(pin))
}
