package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.SettingsDataSource
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.repository.SettingsReader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

/** Чтение настроек из локального хранилища. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<SettingsReader>())
class SettingsReaderImpl(
    private val dataSource: SettingsDataSource,
) : SettingsReader {
    override fun observeSettings(): Flow<AppSettings> = dataSource.settings
}
