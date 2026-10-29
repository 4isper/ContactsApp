package com.m4isper.contactsapp.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Состояние ввода PIN на экране блокировки: цифры, флаги ошибки и проверки.
 * Решения — здесь; запуск проверки и хаптика — на экране. Известная длина
 * (сохраняется установкой) даёт сигнал отправки на последней цифре; без неё
 * работает легаси-режим с ручной отправкой и подсветкой короткого ввода.
 */
internal class LockPinEntry(
    /** Известная длина PIN или null в легаси-режиме установок до обновления. */
    val targetLength: Int?,
) {
    var pin: String by mutableStateOf("")
        private set
    var failed: Boolean by mutableStateOf(false)
        private set
    var checking: Boolean by mutableStateOf(false)
        private set

    /** Пад и кнопка разблокировки активны, пока идёт проверка. */
    val inputEnabled: Boolean
        get() = !checking

    /** Добавляет цифру; true — длина набрана, пора проверять. */
    fun append(digit: Int): Boolean {
        if (checking) return false
        pin = (pin + digit.toString()).take(MAX_PIN_LENGTH)
        failed = false
        return targetLength != null && pin.length == targetLength
    }

    /** Удаляет последнюю цифру и снимает подсветку ошибки. */
    fun delete() {
        if (checking) return
        pin = pin.dropLast(1)
        failed = false
    }

    /**
     * Начинает проверку. false — проверка не началась: в легаси-режиме введено
     * слишком мало цифр, подсветите подсказку о длине.
     */
    fun beginCheck(): Boolean {
        val start = !checking && pin.length >= MIN_PIN_LENGTH
        failed = !checking && !start
        checking = start
        return start
    }

    /** Завершает проверку: успех оставляет ввод, ошибка очищает и подсвечивает. */
    fun finish(success: Boolean) {
        checking = false
        failed = !success
        if (!success) pin = ""
    }
}
