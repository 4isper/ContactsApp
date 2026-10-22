package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** Чтение настроек приложения. */
interface SettingsReader {
    fun observeSettings(): Flow<AppSettings>
}
