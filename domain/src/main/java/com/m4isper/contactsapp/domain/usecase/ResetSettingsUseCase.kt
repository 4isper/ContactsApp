package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.SettingsMaintenance

class ResetSettingsUseCase(
    private val repository: SettingsMaintenance,
) {
    suspend operator fun invoke() = repository.resetSettings()
}
