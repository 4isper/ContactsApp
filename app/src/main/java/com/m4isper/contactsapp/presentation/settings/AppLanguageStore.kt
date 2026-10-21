package com.m4isper.contactsapp.presentation.settings

import android.content.Context

/**
 * Язык приложения в обычных настройках: нужен до создания Activity,
 * поэтому хранится отдельно от основных настроек в DataStore.
 */
interface AppLanguageStore {
    /** Код языка (BCP-47) или пусто, когда язык берётся из системы. */
    var language: String
}

/** Реализация на обычных настройках: значение читается синхронно при старте Activity. */
class SharedPrefsAppLanguageStore(
    context: Context,
) : AppLanguageStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override var language: String
        get() = preferences.getString(KEY_LANGUAGE, "").orEmpty()
        set(value) {
            preferences.edit().putString(KEY_LANGUAGE, value).apply()
        }

    private companion object {
        const val PREFERENCES_NAME = "app_language"
        const val KEY_LANGUAGE = "language"
    }
}
