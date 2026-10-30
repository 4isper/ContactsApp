package com.m4isper.contactsapp.domain.model.settings

/**
 * Время ежедневного напоминания в локальной зоне. Невозможно создать
 * значение вне суток: час 0..23, минута 0..59.
 */
data class ReminderTime(
    val hour: Int,
    val minute: Int,
) {
    init {
        require(hour in MIN_HOUR..MAX_HOUR) { "reminder hour out of range: $hour" }
        require(minute in MIN_MINUTE..MAX_MINUTE) { "reminder minute out of range: $minute" }
    }

    companion object {
        const val MIN_HOUR = 0
        const val MAX_HOUR = 23
        const val MIN_MINUTE = 0
        const val MAX_MINUTE = 59
    }
}
