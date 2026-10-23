package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.ReminderSettings

/** Напоминания о днях рождения и событиях: включение и за сколько дней предупреждать. */
class SetRemindersUseCase(
    private val repository: ReminderSettings,
) {
    suspend operator fun invoke(
        enabled: Boolean,
        leadDays: Int,
    ) = repository.setReminders(enabled, leadDays)
}
