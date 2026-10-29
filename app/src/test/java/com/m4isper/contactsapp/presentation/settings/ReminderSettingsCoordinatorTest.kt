package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetRemindersUseCase
import com.m4isper.contactsapp.presentation.reminders.ReminderScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Настройки напоминаний и будильник не должны расходиться молча. */
class ReminderSettingsCoordinatorTest {
    private class FakeReminderScheduler(
        private val failure: Throwable? = null,
    ) : ReminderScheduler {
        var scheduled = false
        var scheduledAt: ReminderTime? = null
        var cancelled = false

        override fun schedule(at: ReminderTime) {
            failure?.let { throw it }
            scheduled = true
            scheduledAt = at
        }

        override fun cancel() {
            failure?.let { throw it }
            cancelled = true
        }
    }

    private fun coordinator(
        repository: FakeSettingsRepository,
        scheduler: FakeReminderScheduler,
    ) = ReminderSettingsCoordinator(
        setReminders = SetRemindersUseCase(repository),
        readSettings = ObserveSettingsUseCase(repository),
        scheduler = scheduler,
    )

    @Test
    fun `the settings are saved and the alarm is scheduled`() =
        runTest {
            val repository = FakeSettingsRepository()
            val scheduler = FakeReminderScheduler()

            val result =
                coordinator(repository, scheduler).apply(
                    enabled = true,
                    leadDays = 3,
                    time = ReminderTime(9, 0),
                )

            assertTrue(result)
            assertTrue(scheduler.scheduled)
            assertEquals(ReminderTime(9, 0), scheduler.scheduledAt)
            assertEquals(true, repository.settings.value.remindersEnabled)
            assertEquals(3, repository.settings.value.reminderLeadDays)
        }

    @Test
    fun `the chosen time is saved and passed to the scheduler`() =
        runTest {
            val repository = FakeSettingsRepository()
            val scheduler = FakeReminderScheduler()

            val result =
                coordinator(repository, scheduler).apply(
                    enabled = true,
                    leadDays = 7,
                    time = ReminderTime(8, 15),
                )

            assertTrue(result)
            assertEquals(ReminderTime(8, 15), scheduler.scheduledAt)
            assertEquals(ReminderTime(8, 15), repository.settings.value.reminderTime)
        }

    @Test
    fun `a failed scheduling rolls the settings back`() =
        runTest {
            val repository = FakeSettingsRepository()
            val scheduler = FakeReminderScheduler(failure = RuntimeException("no alarm"))

            val result =
                coordinator(repository, scheduler).apply(
                    enabled = true,
                    leadDays = 3,
                    time = ReminderTime(8, 15),
                )

            assertFalse(result)
            assertFalse(scheduler.scheduled)
            // Настройки вернулись к прежним, время тоже: будильник и состояние не расходятся.
            assertEquals(false, repository.settings.value.remindersEnabled)
            assertEquals(ReminderTime(9, 0), repository.settings.value.reminderTime)
        }

    @Test
    fun `a failed cancellation is reported`() =
        runTest {
            val repository = FakeSettingsRepository()
            val scheduler = FakeReminderScheduler(failure = RuntimeException("no alarm"))

            coordinator(repository, scheduler).cancel()

            assertFalse(scheduler.cancelled)
            assertNull(scheduler.scheduledAt)
        }
}
