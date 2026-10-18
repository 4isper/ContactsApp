package com.m4isper.contactsapp.presentation.common.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.m4isper.contactsapp.domain.model.Birthday
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Локализованная дата: с годом — полностью, без года — только день и месяц. */
@Composable
internal fun birthdayText(birthday: Birthday): String {
    val locale = LocalConfiguration.current.locales[0]
    val year = birthday.year
    return if (year != null) {
        LocalDate
            .of(year, birthday.month, birthday.day)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    } else {
        MonthDay
            .of(birthday.month, birthday.day)
            .format(DateTimeFormatter.ofPattern("d MMMM").withLocale(locale))
    }
}
