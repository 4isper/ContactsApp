package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.AppSettings
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
}
