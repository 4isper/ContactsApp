// Подавление осознанное: порт описывает все настройки приложения, дробить его неудобно.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Настройки приложения: тема, сортировка и плотность списка. */
interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setSortOrder(order: ContactSortOrder)

    suspend fun setListDensity(density: ListDensity)

    suspend fun setFavoritesFirst(enabled: Boolean)

    suspend fun setLastContactAccount(account: ContactAccount)

    /** Аккаунт по умолчанию для новых контактов. */
    suspend fun setDefaultContactAccount(account: ContactAccount) = Unit

    /** Язык приложения: пусто — язык системы. */
    suspend fun setAppLanguage(language: String) = Unit

    /** Динамические цвета вместо фирменной палитры. */
    suspend fun setDynamicColor(enabled: Boolean) = Unit

    /** Показывать фотографии контактов. */
    suspend fun setShowPhotos(enabled: Boolean) = Unit

    /** Защита приложения: вкл/выкл и хэш PIN-кода. */
    suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    ) = Unit

    /** Скрывать содержимое окна в недавних задачах. */
    suspend fun setSecureWindow(enabled: Boolean) = Unit

    /** Напоминать о днях рождения и событиях. */
    suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
    ) = Unit

    /** Вернуть настройки к значениям по умолчанию. */
    suspend fun resetSettings() = Unit
}
