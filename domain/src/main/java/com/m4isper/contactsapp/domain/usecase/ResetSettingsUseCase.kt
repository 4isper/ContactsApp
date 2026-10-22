package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.SettingsMaintenance
import dev.zacsweers.metro.Inject

@Inject
class ResetSettingsUseCase(
    private val repository: SettingsMaintenance,
) {
    suspend operator fun invoke() = repository.resetSettings()
}
