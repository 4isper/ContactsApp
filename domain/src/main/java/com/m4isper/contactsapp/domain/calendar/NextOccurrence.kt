package com.m4isper.contactsapp.domain.calendar

import java.time.LocalDate
import java.time.MonthDay

/** Ближайшая дата с указанным днём и месяцем: в этом году или в следующем. */
fun nextOccurrence(
    day: Int,
    month: Int,
    today: LocalDate,
): LocalDate? {
    val monthDay = runCatching { MonthDay.of(month, day) }.getOrNull()
    val thisYear = monthDay?.let { runCatching { it.atYear(today.year) }.getOrNull() }
    return when {
        thisYear == null -> null
        !thisYear.isBefore(today) -> thisYear
        else -> monthDay.let { runCatching { it.atYear(today.year + 1) }.getOrNull() }
    }
}
