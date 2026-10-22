package com.m4isper.contactsapp.domain.reminders

import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactEvent
import com.m4isper.contactsapp.domain.model.EventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UpcomingOccasionsTest {
    private val today = LocalDate.of(2024, 5, 10)

    @Test
    fun `birthday within the lead days is included`() {
        val contact =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                birthday = Birthday(day = 12, month = 5, year = null),
            )

        val upcoming = upcomingOccasions(listOf(contact), leadDays = 7, today = today)

        assertEquals(1, upcoming.size)
        assertEquals(OccasionKind.BIRTHDAY, upcoming.single().kind)
        assertEquals(2, upcoming.single().daysLeft)
        assertEquals(LocalDate.of(2024, 5, 12), upcoming.single().date)
    }

    @Test
    fun `birthday outside the lead days is skipped`() {
        val contact =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                birthday = Birthday(day = 1, month = 6, year = null),
            )

        val upcoming = upcomingOccasions(listOf(contact), leadDays = 7, today = today)

        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun `birthday already passed is moved to the next year`() {
        val contact =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                birthday = Birthday(day = 1, month = 1, year = null),
            )

        val upcoming = upcomingOccasions(listOf(contact), leadDays = 7, today = today)

        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun `events are included with their kind`() {
        val contact =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                events =
                    listOf(
                        ContactEvent(
                            date = Birthday(day = 11, month = 5, year = null),
                            kind = EventKind.ANNIVERSARY,
                        ),
                    ),
            )

        val upcoming = upcomingOccasions(listOf(contact), leadDays = 7, today = today)

        assertEquals(1, upcoming.size)
        assertEquals(OccasionKind.EVENT, upcoming.single().kind)
        assertEquals(EventKind.ANNIVERSARY, upcoming.single().eventKind)
    }

    @Test
    fun `occasions are sorted by the days left`() {
        val soon =
            ContactDetails(
                contactId = 1,
                name = "Bob",
                birthday = Birthday(day = 11, month = 5, year = null),
            )
        val later =
            ContactDetails(
                contactId = 2,
                name = "Anna",
                birthday = Birthday(day = 15, month = 5, year = null),
            )

        val upcoming = upcomingOccasions(listOf(later, soon), leadDays = 7, today = today)

        assertEquals(listOf("Bob", "Anna"), upcoming.map { it.contactName })
    }
}
