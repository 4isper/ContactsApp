package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ReminderSettings
import dev.zacsweers.metro.Inject

/** Напоминания о днях рождения и событиях: включение и за сколько дней предупреждать. */
@Inject
class SetRemindersUseCase(
    private val repository: ReminderSettings,
) {
    suspend operator fun invoke(
        enabled: Boolean,
        leadDays: Int,
    ) = repository.setReminders(enabled, leadDays)
}
