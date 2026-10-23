package com.m4isper.contactsapp.data.format

import com.m4isper.contactsapp.domain.model.contact.Birthday
import java.util.Locale

/**
 * Формат дат ContactsContract: "yyyy-MM-dd", без года — "--MM-dd".
 * Формат принадлежит data-слою; домен хранит только компоненты даты.
 */
fun birthdayToString(birthday: Birthday): String =
    if (birthday.year == null) {
        String.format(Locale.US, "--%02d-%02d", birthday.month, birthday.day)
    } else {
        String.format(Locale.US, "%04d-%02d-%02d", birthday.year, birthday.month, birthday.day)
    }

/** Разбирает "yyyy-MM-dd" или "--MM-dd"; null, если формат не распознан. */
fun birthdayFromString(raw: String?): Birthday? =
    BIRTHDAY_PATTERN
        .matchEntire(raw?.trim().orEmpty())
        ?.let(::birthdayFromMatch)

private val BIRTHDAY_PATTERN = Regex("""(?:(\d{4})-|--)(\d{2})-(\d{2})""")
private val MONTHS = 1..12
private val DAYS = 1..31
private const val YEAR_GROUP = 1
private const val MONTH_GROUP = 2
private const val DAY_GROUP = 3

private fun birthdayFromMatch(match: MatchResult): Birthday? =
    match.groupValues[MONTH_GROUP]
        .toIntOrNull()
        ?.takeIf { it in MONTHS }
        ?.let { month ->
            match.groupValues[DAY_GROUP]
                .toIntOrNull()
                ?.takeIf { it in DAYS }
                ?.let { day ->
                    Birthday(
                        day = day,
                        month = month,
                        year = match.groupValues[YEAR_GROUP].takeIf { it.isNotEmpty() }?.toIntOrNull(),
                    )
                }
        }
