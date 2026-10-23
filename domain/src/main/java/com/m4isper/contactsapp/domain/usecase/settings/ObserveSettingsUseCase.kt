package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.repository.settings.SettingsReader
import kotlinx.coroutines.flow.Flow

class ObserveSettingsUseCase(
    private val repository: SettingsReader,
) {
    operator fun invoke(): Flow<AppSettings> = repository.observeSettings()
}
