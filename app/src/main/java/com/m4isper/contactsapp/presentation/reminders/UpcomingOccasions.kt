package com.m4isper.contactsapp.presentation.reminders

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.EventKind
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/** Тип повода, о котором напоминаем. */
enum class OccasionKind {
    BIRTHDAY,
    EVENT,
}

/** Праздник, который скоро наступит. */
data class UpcomingOccasion(
    val contactId: Long,
    val contactName: String,
    val kind: OccasionKind,
    val eventKind: EventKind? = null,
    val date: LocalDate,
    val daysLeft: Int,
)

/**
 * Ближайшие дни рождения и события: то, о чём стоит напомнить за [leadDays] дней.
 * Даты без года считаются ежегодными.
 */
fun upcomingOccasions(
    contacts: List<ContactDetails>,
    leadDays: Int,
    today: LocalDate = LocalDate.now(),
): List<UpcomingOccasion> {
    val horizon = today.plusDays(leadDays.toLong())
    val occasions = mutableListOf<UpcomingOccasion>()
    contacts.forEach { contact ->
        contact.birthday?.let { birthday ->
            nextOccurrence(birthday.day, birthday.month, today)?.let { date ->
                addIfSoon(occasions, contact, OccasionKind.BIRTHDAY, null, date, today, horizon)
            }
        }
        contact.events.forEach { event ->
            val date = event.date
            nextOccurrence(date.day, date.month, today)?.let { next ->
                addIfSoon(occasions, contact, OccasionKind.EVENT, event.kind, next, today, horizon)
            }
        }
    }
    return occasions.sortedWith(compareBy({ it.daysLeft }, { it.contactName }))
}

/** Ближайшая дата с указанным днём и месяцем: в этом году или в следующем. */
private fun nextOccurrence(
    day: Int,
    month: Int,
    today: LocalDate,
): LocalDate? {
    val monthDay = runCatching { MonthDay.of(month, day) }.getOrNull() ?: return null
    val thisYear = runCatching { monthDay.atYear(today.year) }.getOrNull()
    return when {
        thisYear == null -> null
        !thisYear.isBefore(today) -> thisYear
        else -> runCatching { monthDay.atYear(today.year + 1) }.getOrNull()
    }
}

private fun addIfSoon(
    target: MutableList<UpcomingOccasion>,
    contact: ContactDetails,
    kind: OccasionKind,
    eventKind: EventKind?,
    date: LocalDate,
    today: LocalDate,
    horizon: LocalDate,
) {
    if (date.isAfter(horizon)) return
    val daysLeft = ChronoUnit.DAYS.between(today, date).toInt()
    if (daysLeft < 0) return
    target +=
        UpcomingOccasion(
            contactId = contact.contactId,
            contactName = contact.name,
            kind = kind,
            eventKind = eventKind,
            date = date,
            daysLeft = daysLeft,
        )
}
