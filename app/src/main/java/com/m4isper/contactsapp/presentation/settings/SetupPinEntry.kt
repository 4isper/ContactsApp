package com.m4isper.contactsapp.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Шаг установки PIN: сначала код, затем его повтор. */
enum class SetupStage {
    FIRST,
    REPEAT,
}

/**
 * Состояние двухшаговой установки PIN: длина выбирается пользователем, поэтому
 * переход между шагами — по клавише подтверждения, а не по автоотправке.
 * Несовпадение на повторе подсвечивается, когда ввод достиг длины первого кода.
 */
internal class SetupPinEntry {
    var stage: SetupStage by mutableStateOf(SetupStage.FIRST)
        private set
    var pin: String by mutableStateOf("")
        private set
    /** Код с первого шага; на нём же хранится итог установки. */
    var first: String by mutableStateOf("")
        private set
    var shortFlash: Boolean by mutableStateOf(false)
        private set
    var mismatch: Boolean by mutableStateOf(false)
        private set

    /** Пара совпала и достаточно длинна: можно сохранять. */
    val done: Boolean
        get() = stage == SetupStage.REPEAT && pin.isNotEmpty() && pin == first

    fun append(digit: Int) {
        pin = (pin + digit.toString()).take(MAX_PIN_LENGTH)
        updateMismatch()
    }

    fun delete() {
        pin = pin.dropLast(1)
        updateMismatch()
    }

    /** Подтверждает первый шаг; false — короче минимума, подсветите подсказку. */
    fun confirmFirst(): Boolean {
        val ok = pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH
        shortFlash = !ok
        if (ok) {
            first = pin
            pin = ""
            stage = SetupStage.REPEAT
        }
        return ok
    }

    /** Подтверждает пару; false — повтор не совпал, подсветите несовпадение. */
    fun confirmPair(): Boolean = done

    private fun updateMismatch() {
        mismatch = stage == SetupStage.REPEAT && pin.length == first.length && pin != first
    }
}
