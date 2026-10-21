package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject

@Inject
class ResetSettingsUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke() = repository.resetSettings()
}
