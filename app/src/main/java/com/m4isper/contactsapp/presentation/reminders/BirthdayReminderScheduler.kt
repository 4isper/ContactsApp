package com.m4isper.contactsapp.presentation.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import java.time.Instant
import java.time.ZoneId

/**
 * Ежедневное напоминание о днях рождения и событиях: планировщик ставит будильник,
 * получатель по нему собирает список праздников и показывает уведомление.
 */
interface ReminderScheduler {
    /** Ставит ежедневный будильник на выбранное время суток. */
    fun schedule(at: ReminderTime)

    /** Убирает будильник: напоминания выключены. */
    fun cancel()
}

/**
 * Следующий момент срабатывания ежедневного напоминания: раньше выбранного времени —
 * сегодня, в самый момент и позже — завтра. Чистая функция: дата считается в локальной
 * зоне, поэтому напоминание остаётся на том же времени суток и при переводе часов.
 */
fun nextReminderTrigger(
    nowMillis: Long,
    at: ReminderTime,
    zone: ZoneId,
): Long {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)

    fun localTimeOn(daysToAdd: Long = 0): Long =
        now
            .toLocalDate()
            .plusDays(daysToAdd)
            .atTime(at.hour, at.minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    return if (localTimeOn() <= nowMillis) localTimeOn(daysToAdd = 1) else localTimeOn()
}

/** Планировщик на базе AlarmManager. */
class BirthdayReminderScheduler(
    private val context: Context,
) : ReminderScheduler {
    override fun schedule(at: ReminderTime) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger = nextReminderTrigger(System.currentTimeMillis(), at, ZoneId.systemDefault())
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
    }
}
