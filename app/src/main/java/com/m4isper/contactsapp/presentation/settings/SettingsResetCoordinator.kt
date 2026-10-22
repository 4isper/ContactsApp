package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.domain.usecase.ResetSettingsUseCase
import dev.zacsweers.metro.Inject

/**
 * Сброс настроек: язык приложения, будильник напоминания и сами настройки.
 * Экран не знает про хранилище языка и про планировщик - он зовёт координатор.
 */
@Inject
internal class SettingsResetCoordinator(
    private val resetSettings: ResetSettingsUseCase,
    private val languageStore: AppLanguageStore,
    private val reminders: ReminderSettingsCoordinator,
) {
    /** Возвращает настройки к значениям по умолчанию. */
    suspend fun reset() {
        languageStore.language = ""
        reminders.cancel()
        resetSettings()
    }
}
