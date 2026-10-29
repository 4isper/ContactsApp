package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.domain.usecase.settings.ResetSettingsUseCase
import dev.zacsweers.metro.Inject

/**
 * Сброс настроек: язык приложения, системный выбор языка, будильник напоминания
 * и сами настройки. Экран не знает про хранилища языка и планировщик - он зовёт
 * координатор. Системный выбор языка тоже сбрасывается: иначе после сброса
 * система вернула бы сброшенный язык обратно.
 */
@Inject
internal class SettingsResetCoordinator(
    private val resetSettings: ResetSettingsUseCase,
    private val languageStore: AppLanguageStore,
    private val system: SystemAppLanguage,
    private val reminders: ReminderSettingsCoordinator,
) {
    /** Возвращает настройки к значениям по умолчанию. */
    suspend fun reset() {
        languageStore.language = ""
        system.set("")
        reminders.cancel()
        resetSettings()
    }
}
