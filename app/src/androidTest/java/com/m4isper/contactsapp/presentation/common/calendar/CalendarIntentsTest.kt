package com.m4isper.contactsapp.presentation.common.calendar

import android.content.Intent
import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** Действие «добавить в календарь»: запрос на создание события. */
@RunWith(AndroidJUnit4::class)
class CalendarIntentsTest {
    @Test
    fun intent_asks_to_create_an_event() {
        val date = LocalDate.of(2024, 5, 12)

        val intent = buildCalendarEventIntent("Anna — birthday", date)

        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, intent.data)
        assertEquals("Anna — birthday", intent.getStringExtra(CalendarContract.Events.TITLE))
        assertTrue(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
    }

    @Test
    fun intent_starts_at_nine_o_clock() {
        val date = LocalDate.of(2024, 5, 12)

        val intent = buildCalendarEventIntent("Anna — birthday", date)

        val expected =
            date
                .atTime(9, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        assertEquals(expected, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0L))
    }

    @Test
    fun occurrence_is_in_this_year_when_it_has_not_passed() {
        val today = LocalDate.of(2024, 5, 10)

        val date = nextOccurrence(day = 12, month = 5, today = today)

        assertEquals(LocalDate.of(2024, 5, 12), date)
    }

    @Test
    fun occurrence_moves_to_next_year_when_it_has_passed() {
        val today = LocalDate.of(2024, 5, 10)

        val date = nextOccurrence(day = 1, month = 1, today = today)

        assertEquals(LocalDate.of(2025, 1, 1), date)
    }

    @Test
    fun occurrence_is_null_for_an_impossible_date() {
        assertNull(nextOccurrence(day = 31, month = 2, today = LocalDate.of(2024, 5, 10)))
    }

    @Test
    fun occurrence_handles_the_leap_day() {
        val today = LocalDate.of(2024, 2, 28)

        val date = nextOccurrence(day = 29, month = 2, today = today)

        assertNotNull(date)
        assertEquals(LocalDate.of(2024, 2, 29), date)
    }
}
