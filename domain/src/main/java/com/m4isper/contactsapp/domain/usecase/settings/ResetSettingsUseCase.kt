package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.SettingsMaintenance

class ResetSettingsUseCase(
    private val repository: SettingsMaintenance,
) {
    suspend operator fun invoke() = repository.resetSettings()
}
