package com.m4isper.contactsapp.presentation.common

import android.util.Log
import com.m4isper.contactsapp.BuildConfig

/** Сколько символов сообщения попадает в лог. */
private const val MAX_LOG_LENGTH = 512

/** Цифры в тексте ошибки скрыты: там могут быть телефоны и id контактов. */
private val DIGITS = Regex("\\d")

internal inline fun logd(
    tag: String,
    message: () -> String,
) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, message())
    }
}

/**
 * Предупреждение: пишется и в релизной сборке, текст ошибки без цифр.
 * Логи приложения читает только сам телефон и разработчик по adb.
 */
internal fun logw(
    tag: String,
    message: String,
    error: Throwable,
) {
    loge(tag, message, error)
}

/** Ошибка: пишется и в релизной сборке, текст ошибки без цифр. */
internal fun loge(
    tag: String,
    message: String,
    error: Throwable,
) {
    Log.w(tag, "$message: ${error.redacted()}")
}

/** Текст ошибки без цифр и не длиннее [MAX_LOG_LENGTH]. */
internal fun Throwable.redacted(): String =
    toString()
        .replace(DIGITS, "#")
        .take(MAX_LOG_LENGTH)
