package com.m4isper.contactsapp.presentation.contacts.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSyncFormattingTest {
    @Test
    fun `less than a minute is just now`() {
        assertEquals(SyncRelativeTime.JustNow, syncRelativeTime(lastSyncTimeMs = 1_000L, nowMs = 60_999L))
    }

    @Test
    fun `future timestamp is clamped to just now`() {
        assertEquals(SyncRelativeTime.JustNow, syncRelativeTime(lastSyncTimeMs = 10_000L, nowMs = 5_000L))
    }

    @Test
    fun `exactly one minute is one minute ago`() {
        assertEquals(SyncRelativeTime.Minutes(1), syncRelativeTime(lastSyncTimeMs = 0L, nowMs = 60_000L))
    }

    @Test
    fun `last minute before an hour is still minutes`() {
        assertEquals(
            SyncRelativeTime.Minutes(59),
            syncRelativeTime(lastSyncTimeMs = 0L, nowMs = 59 * 60_000L + 59_999L),
        )
    }

    @Test
    fun `exactly one hour is one hour ago`() {
        assertEquals(SyncRelativeTime.Hours(1), syncRelativeTime(lastSyncTimeMs = 0L, nowMs = 60 * 60_000L))
    }

    @Test
    fun `last hour before a day is still hours`() {
        assertEquals(
            SyncRelativeTime.Hours(23),
            syncRelativeTime(lastSyncTimeMs = 0L, nowMs = 24 * 60 * 60_000L - 1L),
        )
    }

    @Test
    fun `exactly one day is one day ago`() {
        assertEquals(SyncRelativeTime.Days(1), syncRelativeTime(lastSyncTimeMs = 0L, nowMs = 24 * 60 * 60_000L))
    }

    @Test
    fun `several days are reported as days`() {
        assertEquals(
            SyncRelativeTime.Days(7),
            syncRelativeTime(lastSyncTimeMs = 0L, nowMs = 7 * 24 * 60 * 60_000L),
        )
    }
}
