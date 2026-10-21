package com.m4isper.contactsapp.presentation.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Ежедневное напоминание о днях рождения и событиях: планировщик ставит будильник,
 * получатель по нему собирает список праздников и показывает уведомление.
 */
interface ReminderScheduler {
    /** Ставит ежедневный будильник на утро. */
    fun schedule()

    /** Убирает будильник: напоминания выключены. */
    fun cancel()
}

/** Планировщик на базе AlarmManager. */
class BirthdayReminderScheduler(
    private val context: Context,
) : ReminderScheduler {
    override fun schedule() {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger =
            Calendar
                .getInstance()
                .apply {
                    set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }.timeInMillis
        manager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(),
        )
    }

    override fun cancel() {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BirthdayReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val REQUEST_CODE = 4201
        const val REMINDER_HOUR = 9
    }
}
