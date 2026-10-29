package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.repository.settings.ReminderSettings

/**
 * Напоминания о днях рождения и событиях: включение, за сколько дней предупреждать
 * и в какое время суток.
 */
class SetRemindersUseCase(
    private val repository: ReminderSettings,
) {
    suspend operator fun invoke(
        enabled: Boolean,
        leadDays: Int,
        time: ReminderTime,
    ) = repository.setReminders(enabled, leadDays, time)
}
