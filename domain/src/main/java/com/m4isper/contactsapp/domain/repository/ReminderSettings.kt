package com.m4isper.contactsapp.domain.repository

/** Напоминания о днях рождения и событиях. */
interface ReminderSettings {
    suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
    )
}
