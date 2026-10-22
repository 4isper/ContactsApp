package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.SetRemindersUseCase
import com.m4isper.contactsapp.presentation.reminders.ReminderScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Настройки напоминаний и будильник не должны расходиться молча. */
class ReminderSettingsCoordinatorTest {
    private class FakeReminderScheduler(
        private val failure: Throwable? = null,
    ) : ReminderScheduler {
        var scheduled = false
        var cancelled = false

        override fun schedule() {
            failure?.let { throw it }
            scheduled = true
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

            val result = coordinator(repository, scheduler).apply(enabled = true, leadDays = 3)

            assertTrue(result)
            assertTrue(scheduler.scheduled)
            assertEquals(true, repository.settings.value.remindersEnabled)
            assertEquals(3, repository.settings.value.reminderLeadDays)
        }

    @Test
    fun `a failed scheduling rolls the settings back`() =
        runTest {
            val repository = FakeSettingsRepository()
            val scheduler = FakeReminderScheduler(failure = RuntimeException("no alarm"))

            val result = coordinator(repository, scheduler).apply(enabled = true, leadDays = 3)

            assertFalse(result)
            assertFalse(scheduler.scheduled)
            // Настройки вернулись к прежним: будильник и состояние не расходятся.
            assertEquals(false, repository.settings.value.remindersEnabled)
        }

    @Test
    fun `a failed cancellation is reported`() =
        runTest {
            val repository = FakeSettingsRepository()
            val scheduler = FakeReminderScheduler(failure = RuntimeException("no alarm"))

            coordinator(repository, scheduler).cancel()

            assertFalse(scheduler.cancelled)
        }
}
