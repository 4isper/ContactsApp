package com.m4isper.contactsapp.domain.repository.settings

/** Напоминания о днях рождения и событиях. */
interface ReminderSettings {
    suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
    )
}
