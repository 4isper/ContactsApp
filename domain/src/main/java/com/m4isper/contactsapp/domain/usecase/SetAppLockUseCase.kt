package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.hashAppLockPin
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Защита приложения: вкл/выкл и PIN-код (в настройки попадает только хэш). */
@Inject
class SetAppLockUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(
        enabled: Boolean,
        pin: String,
    ) = withContext(Dispatchers.Default) {
        val hash = if (enabled && pin.isNotBlank()) hashAppLockPin(pin) else ""
        repository.setAppLock(enabled, hash)
    }
}
