package com.m4isper.contactsapp.domain.repository.settings

import com.m4isper.contactsapp.domain.model.settings.AppSettings
import kotlinx.coroutines.flow.Flow

/** Чтение настроек приложения. */
interface SettingsReader {
    fun observeSettings(): Flow<AppSettings>
}
