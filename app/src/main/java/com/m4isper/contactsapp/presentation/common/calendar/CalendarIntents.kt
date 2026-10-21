package com.m4isper.contactsapp.presentation.common.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.m4isper.contactsapp.presentation.common.logw
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId

private const val CALENDAR_TAG = "CalendarIntents"

/**
 * Открывает создание события в календаре: дни рождения и памятные даты
 * добавляются одним нажатием.
 */
internal fun openCalendarEvent(
    context: Context,
    title: String,
    date: LocalDate,
) {
    val intent = buildCalendarEventIntent(title, date)
    runCatching { context.startActivity(intent) }
        .onFailure { logw(CALENDAR_TAG, "open calendar failed", it) }
}

/** Запрос на создание события: название, дата и целый день. */
internal fun buildCalendarEventIntent(
    title: String,
    date: LocalDate,
): Intent {
    val start =
        date
            .atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    return Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + HOUR_MILLIS)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/** Ближайшая дата с указанным днём и месяцем: в этом году или в следующем. */
fun nextOccurrence(
    day: Int,
    month: Int,
    today: LocalDate = LocalDate.now(),
): LocalDate? {
    val monthDay = runCatching { MonthDay.of(month, day) }.getOrNull()
    val thisYear = monthDay?.let { runCatching { it.atYear(today.year) }.getOrNull() }
    return when {
        thisYear == null -> null
        !thisYear.isBefore(today) -> thisYear
        else -> monthDay?.let { runCatching { it.atYear(today.year + 1) }.getOrNull() }
    }
}

private const val HOUR_MILLIS = 60L * 60 * 1000
