package com.m4isper.contactsapp.domain.repository.settings

/** Обслуживание настроек: возврат к значениям по умолчанию. */
interface SettingsMaintenance {
    /** Вернуть настройки к значениям по умолчанию. */
    suspend fun resetSettings()
}
