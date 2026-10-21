package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import kotlinx.coroutines.flow.Flow

/** Настройки приложения в локальном хранилище. */
interface SettingsDataSource {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: String)

    suspend fun setSortOrder(order: String)

    suspend fun setListDensity(density: String)

    suspend fun setFavoritesFirst(enabled: Boolean)

    suspend fun setLastContactAccount(account: ContactAccount)
}
