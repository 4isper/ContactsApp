package com.m4isper.contactsapp.presentation.editor.dialogs

import com.m4isper.contactsapp.domain.model.contact.Birthday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class BirthdayPickerResultTest {
    private fun millisOf(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `picked date keeps its year when no-year is off`() {
        val result =
            pickerBirthdayResult(
                selectedDateMillis = millisOf(LocalDate.of(1985, 7, 21)),
                birthday = null,
                initialYear = 1985,
                noYear = false,
            )

        assertEquals(Birthday(day = 21, month = 7, year = 1985), result)
    }

    @Test
    fun `picked date drops the year in no-year mode`() {
        val result =
            pickerBirthdayResult(
                selectedDateMillis = millisOf(LocalDate.of(2000, 7, 21)),
                birthday = Birthday(21, 7, year = null),
                initialYear = 2000,
                noYear = true,
            )

        assertEquals(Birthday(day = 21, month = 7, year = null), result)
    }

    @Test
    fun `no selection falls back to the stored value with its own year`() {
        val result =
            pickerBirthdayResult(
                selectedDateMillis = null,
                birthday = Birthday(9, 12, year = 1991),
                initialYear = 1991,
                noYear = false,
            )

        assertEquals(Birthday(day = 9, month = 12, year = 1991), result)
    }

    @Test
    fun `no selection on a no-year birthday confirms without a year`() {
        val result =
            pickerBirthdayResult(
                selectedDateMillis = null,
                birthday = Birthday(9, 12, year = null),
                initialYear = 2000,
                noYear = true,
            )

        assertEquals(Birthday(day = 9, month = 12, year = null), result)
    }

    @Test
    fun `empty state without selection confirms nothing`() {
        val result =
            pickerBirthdayResult(
                selectedDateMillis = null,
                birthday = null,
                initialYear = 2000,
                noYear = false,
            )

        assertNull(result)
    }

    @Test
    fun `initial year uses the stored year when present`() {
        assertEquals(1985, pickerInitialYear(Birthday(1, 1, year = 1985), startNoYear = true))
    }

    @Test
    fun `initial year is the synthetic leap-safe year in no-year mode`() {
        assertEquals(2000, pickerInitialYear(Birthday(1, 1, year = null), startNoYear = true))
    }

    @Test
    fun `initial year defaults to the current year when a year is expected`() {
        assertEquals(LocalDate.now().year, pickerInitialYear(Birthday(1, 1, year = null), startNoYear = false))
        assertEquals(LocalDate.now().year, pickerInitialYear(null, startNoYear = false))
    }
}
