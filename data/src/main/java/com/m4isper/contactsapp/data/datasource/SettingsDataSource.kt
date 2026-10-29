// Подавление осознанное: набор настроек приложения растёт, дробить порт неудобно.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import kotlinx.coroutines.flow.Flow

/** Настройки приложения в локальном хранилище. */
interface SettingsDataSource {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: String)

    suspend fun setSortOrder(order: String)

    suspend fun setListDensity(density: String)

    suspend fun setFavoritesFirst(enabled: Boolean)

    suspend fun setLastContactAccount(account: ContactAccount)

    /** Аккаунт по умолчанию для новых контактов. */
    suspend fun setDefaultContactAccount(account: ContactAccount)

    /** Язык приложения: пусто — язык системы. */
    suspend fun setAppLanguage(language: String)

    /** Динамические цвета вместо фирменной палитры. */
    suspend fun setDynamicColor(enabled: Boolean)

    /** Показывать фотографии контактов. */
    suspend fun setShowPhotos(enabled: Boolean)

    /** Защита приложения: вкл/выкл и хэш PIN-кода. */
    suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    )

    /** Скрывать содержимое окна в недавних задачах. */
    suspend fun setSecureWindow(enabled: Boolean)

    /** Напоминать о днях рождения и событиях. */
    suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
        time: ReminderTime,
    )

    /** Вернуть настройки к значениям по умолчанию. */
    suspend fun resetSettings()
}
