package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject

@Inject
class SetDynamicColorUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDynamicColor(enabled)
}
