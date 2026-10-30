package com.m4isper.contactsapp.presentation.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * Язык, выбранный для приложения на системной странице «Язык приложения»
 * (Android 13+). Ниже Android 13 системного выбора нет и все операции пустые.
 */
interface SystemAppLanguage {
    /** BCP-47 тег или пусто, когда выбран язык системы. */
    fun current(): String

    /** Зеркалит выбор приложения в систему; ниже Android 13 ничего не делает. */
    fun set(language: String)
}

/** Реализация на LocaleManager: система хранит выбор и сама применяет его к контекстам. */
class LocaleManagerSystemAppLanguage(
    private val context: Context,
) : SystemAppLanguage {
    override fun current(): String =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ""
        } else {
            localeManager()
                ?.applicationLocales
                ?.takeIf { !it.isEmpty }
                ?.toLanguageTags()
                .orEmpty()
        }

    override fun set(language: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = localeManager() ?: return
        manager.applicationLocales =
            if (language.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun localeManager(): LocaleManager? = context.getSystemService(LocaleManager::class.java)
}

/**
 * Контекст на языке приложения — единое решение для окна, виджета и уведомлений.
 * На Android 13+ системный выбор сильнее: система уже применила его к контексту,
 * и ручная обёртка только конфликтовала бы с ним. Ниже — язык из хранилища,
 * который нужен до создания Activity, поэтому он живёт не в основных настройках.
 */
fun Context.withAppLanguage(): Context {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        LocaleManagerSystemAppLanguage(this).current().isNotEmpty()
    ) {
        // Система уже применила свой выбор к контексту: обёртка конфликтовала бы с ним.
        return this
    }
    val language = SharedPrefsAppLanguageStore(this).language
    return if (language.isBlank()) this else language.wrapLocale(this)
}

/** Оборачивает контекст выбранным языком: локаль и направление письма. */
private fun String.wrapLocale(base: Context): Context {
    val locale = Locale.forLanguageTag(this)
    Locale.setDefault(locale)
    val configuration = Configuration(base.resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return base.createConfigurationContext(configuration)
}
