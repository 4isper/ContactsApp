package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.domain.usecase.settings.SetAppLanguageUseCase
import dev.zacsweers.metro.Inject

/**
 * Язык приложения: хранится и в настройках, и в общих настройках - он нужен
 * до запуска Activity. Экран работает через координатор, а не через хранилище.
 */
@Inject
internal class AppLanguageCoordinator(
    private val setLanguage: SetAppLanguageUseCase,
    private val languageStore: AppLanguageStore,
) {
    /** Сохраняет выбранный язык. */
    suspend fun set(language: String) {
        languageStore.language = language
        setLanguage(language)
    }
}
