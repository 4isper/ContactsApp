package com.m4isper.contactsapp.domain.repository.settings

import com.m4isper.contactsapp.domain.model.settings.ReminderTime

/** Напоминания о днях рождения и событиях. */
interface ReminderSettings {
    suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
        time: ReminderTime,
    )
}
