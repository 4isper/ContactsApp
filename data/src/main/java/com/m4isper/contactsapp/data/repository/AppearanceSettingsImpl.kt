package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.SettingsDataSource
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.repository.settings.AppearanceSettings
import com.m4isper.contactsapp.domain.repository.settings.LanguageSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/** Внешний вид и язык приложения в локальном хранилище. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<AppearanceSettings>())
@ContributesBinding(AppScope::class, binding = binding<LanguageSettings>())
class AppearanceSettingsImpl(
    private val dataSource: SettingsDataSource,
) : AppearanceSettings,
    LanguageSettings {
    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode.name)

    override suspend fun setDynamicColor(enabled: Boolean) = dataSource.setDynamicColor(enabled)

    override suspend fun setShowPhotos(enabled: Boolean) = dataSource.setShowPhotos(enabled)

    override suspend fun setAppLanguage(language: String) = dataSource.setAppLanguage(language)
}
