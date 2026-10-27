package com.m4isper.contactsapp.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class RemindersDeliveryTest {
    @Test
    fun `switch is on only when preference and delivery agree`() {
        assertEquals(
            true,
            remindersEffectivelyOn(enabled = true, appNotificationsEnabled = true, channelBlocked = false),
        )
        assertEquals(
            false,
            remindersEffectivelyOn(enabled = true, appNotificationsEnabled = false, channelBlocked = false),
        )
        assertEquals(
            false,
            remindersEffectivelyOn(enabled = true, appNotificationsEnabled = true, channelBlocked = true),
        )
        assertEquals(
            false,
            remindersEffectivelyOn(enabled = false, appNotificationsEnabled = true, channelBlocked = false),
        )
    }
}
