package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetRemindersUseCase
import com.m4isper.contactsapp.presentation.common.safeCall
import com.m4isper.contactsapp.presentation.reminders.ReminderScheduler
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

private const val REMINDERS_TAG = "ReminderSettings"

/**
 * Настройки напоминаний: сначала сохраняем, потом планируем будильник. Если
 * планирование не удалось, прежние настройки возвращаются на место и сбой
 * возвращается вызывающему - сохранённое состояние и будильник не расходятся
 * молча.
 */
@Inject
internal class ReminderSettingsCoordinator(
    private val setReminders: SetRemindersUseCase,
    private val readSettings: ObserveSettingsUseCase,
    private val scheduler: ReminderScheduler,
) {
    /**
     * Сохраняет настройки и планирует будильник.
     * @return true, если будильник соответствует сохранённым настройкам.
     */
    suspend fun apply(
        enabled: Boolean,
        leadDays: Int,
        time: ReminderTime,
    ): Boolean {
        val previous = readSettings().first()
        setReminders(enabled, leadDays, time)
        val scheduled =
            safeCall(REMINDERS_TAG, "schedule reminders failed") {
                if (enabled) scheduler.schedule(time) else scheduler.cancel()
            }.isSuccess
        if (!scheduled) {
            // Будильник не поставлен: возвращаем прежние настройки, чтобы они не расходились.
            safeCall(REMINDERS_TAG, "rollback reminders failed") {
                setReminders(previous.remindersEnabled, previous.reminderLeadDays, previous.reminderTime)
            }
        }
        return scheduled
    }

    /** Убирает будильник: напоминания выключены. */
    suspend fun cancel() {
        safeCall(REMINDERS_TAG, "cancel reminders failed") { scheduler.cancel() }
    }
}
