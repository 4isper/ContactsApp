package com.m4isper.contactsapp.presentation.settings

import android.os.Build
import com.m4isper.contactsapp.domain.usecase.settings.SetAppLanguageUseCase
import dev.zacsweers.metro.Inject

/**
 * Язык приложения: хранится и в настройках, и в общих настройках - он нужен
 * до запуска Activity. На Android 13+ выбор зеркалится в систему, чтобы
 * системная страница «Язык приложения» показывала то же самое.
 */
@Inject
internal class AppLanguageCoordinator(
    private val setLanguage: SetAppLanguageUseCase,
    private val languageStore: AppLanguageStore,
    private val system: SystemAppLanguage,
) {
    /**
     * Пересоздавать ли окно вручную: ниже Android 13 система не применяет язык
     * сама, выше — пересоздаёт после setApplicationLocales, и ручной вызов
     * дал бы двойное пересоздание.
     */
    val restartRequired: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    /** Сохраняет выбранный язык: хранилище, настройки и зеркало в систему. */
    suspend fun set(language: String) {
        languageStore.language = language
        setLanguage(language)
        system.set(language)
    }
}
