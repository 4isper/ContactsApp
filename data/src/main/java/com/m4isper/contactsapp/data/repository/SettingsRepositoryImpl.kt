// Подавление осознанное: набор настроек приложения растёт, дробить порт неудобно.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.SettingsDataSource
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SettingsRepositoryImpl(
    private val dataSource: SettingsDataSource,
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = dataSource.settings

    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode.name)

    override suspend fun setSortOrder(order: ContactSortOrder) = dataSource.setSortOrder(order.name)

    override suspend fun setListDensity(density: ListDensity) = dataSource.setListDensity(density.name)

    override suspend fun setFavoritesFirst(enabled: Boolean) = dataSource.setFavoritesFirst(enabled)

    override suspend fun setLastContactAccount(account: ContactAccount) = dataSource.setLastContactAccount(account)

    override suspend fun setDefaultContactAccount(account: ContactAccount) =
        dataSource.setDefaultContactAccount(account)

    override suspend fun setAppLanguage(language: String) = dataSource.setAppLanguage(language)

    override suspend fun setDynamicColor(enabled: Boolean) = dataSource.setDynamicColor(enabled)

    override suspend fun setShowPhotos(enabled: Boolean) = dataSource.setShowPhotos(enabled)

    override suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    ) = dataSource.setAppLock(enabled, pinHash)

    override suspend fun setSecureWindow(enabled: Boolean) = dataSource.setSecureWindow(enabled)

    override suspend fun resetSettings() = dataSource.resetSettings()
}
