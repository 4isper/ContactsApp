package com.m4isper.contactsapp.presentation.reminders

import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Расчёт следующего срабатывания ежедневного напоминания: раньше времени — сегодня,
 * в момент срабатывания и позже — завтра. Зона фиксированная, тест не зависит от машины.
 */
class NextReminderTriggerTest {
    private val zone = ZoneId.of("Europe/Moscow")

    @Test
    fun `before the chosen time the trigger is today`() {
        val now = at(2026, 10, 29, 7, 0)

        val trigger = nextReminderTrigger(now.toInstant().toEpochMilli(), ReminderTime(9, 0), zone)

        assertEquals(at(2026, 10, 29, 9, 0).toInstant().toEpochMilli(), trigger)
    }

    @Test
    fun `minutes of the chosen time are honored`() {
        val now = at(2026, 10, 29, 7, 0)

        val trigger = nextReminderTrigger(now.toInstant().toEpochMilli(), ReminderTime(8, 15), zone)

        assertEquals(at(2026, 10, 29, 8, 15).toInstant().toEpochMilli(), trigger)
    }

    @Test
    fun `after the chosen time the trigger is tomorrow`() {
        val now = at(2026, 10, 29, 9, 1)

        val trigger = nextReminderTrigger(now.toInstant().toEpochMilli(), ReminderTime(9, 0), zone)

        assertEquals(at(2026, 10, 30, 9, 0).toInstant().toEpochMilli(), trigger)
    }

    @Test
    fun `exactly at the chosen time the trigger is tomorrow`() {
        val now = at(2026, 10, 29, 9, 0)

        val trigger = nextReminderTrigger(now.toInstant().toEpochMilli(), ReminderTime(9, 0), zone)

        assertEquals(at(2026, 10, 30, 9, 0).toInstant().toEpochMilli(), trigger)
    }

    @Test
    fun `a midnight reminder schedules for the next midnight`() {
        val now = at(2026, 10, 29, 23, 30)

        val trigger = nextReminderTrigger(now.toInstant().toEpochMilli(), ReminderTime(0, 0), zone)

        assertEquals(at(2026, 10, 30, 0, 0).toInstant().toEpochMilli(), trigger)
    }

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): ZonedDateTime = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
}
