package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.repository.SettingsReader
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class ObserveSettingsUseCase(
    private val repository: SettingsReader,
) {
    operator fun invoke(): Flow<AppSettings> = repository.observeSettings()
}
