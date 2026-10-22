package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.SettingsDataSource
import com.m4isper.contactsapp.domain.repository.PrivacySettings
import com.m4isper.contactsapp.domain.repository.ReminderSettings
import com.m4isper.contactsapp.domain.repository.SettingsMaintenance
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/** Приватность, напоминания и сброс настроек в локальном хранилище. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<PrivacySettings>())
@ContributesBinding(AppScope::class, binding = binding<ReminderSettings>())
@ContributesBinding(AppScope::class, binding = binding<SettingsMaintenance>())
class PrivacySettingsImpl(
    private val dataSource: SettingsDataSource,
) : PrivacySettings,
    ReminderSettings,
    SettingsMaintenance {
    override suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    ) = dataSource.setAppLock(enabled, pinHash)

    override suspend fun setSecureWindow(enabled: Boolean) = dataSource.setSecureWindow(enabled)

    override suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
    ) = dataSource.setReminders(enabled, leadDays)

    override suspend fun resetSettings() = dataSource.resetSettings()
}
