package com.m4isper.contactsapp.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Защита от перебора PIN: после порога неудач ввод блокируется. */
class AppLockAttemptsTest {
    @Test
    fun `attempts below the limit do not block the input`() {
        val attempts = AppLockAttempts(clock = TestClock())

        repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS - 1) { attempts.registerFailure() }

        assertFalse(attempts.isLocked())
        assertEquals(Duration.ZERO, attempts.remainingLock())
    }

    @Test
    fun `the input is blocked after the limit of failed attempts`() {
        val attempts = AppLockAttempts(clock = TestClock())

        repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS) { attempts.registerFailure() }

        assertTrue(attempts.isLocked())
        assertEquals(AppLockAttempts.DEFAULT_BASE_DELAY, attempts.remainingLock())
    }

    @Test
    fun `every further failure lengthens the pause`() {
        val attempts = AppLockAttempts(clock = TestClock())

        repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS) { attempts.registerFailure() }
        val first = attempts.remainingLock()
        attempts.registerFailure()

        assertTrue(attempts.remainingLock() > first)
    }

    @Test
    fun `the pause is capped`() {
        val attempts = AppLockAttempts(clock = TestClock())

        repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS + 20) { attempts.registerFailure() }

        assertEquals(AppLockAttempts.DEFAULT_MAX_DELAY, attempts.remainingLock())
    }

    @Test
    fun `a successful attempt resets the counter`() {
        val attempts = AppLockAttempts(clock = TestClock())
        repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS) { attempts.registerFailure() }

        attempts.registerSuccess()

        assertFalse(attempts.isLocked())
        repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS - 1) { attempts.registerFailure() }
        assertFalse(attempts.isLocked())
    }

    @Test
    fun `the block expires after the pause`() {
        val clock = TestClock()
        val attempts = AppLockAttempts(clock = clock)
        repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS) { attempts.registerFailure() }

        clock.advance(AppLockAttempts.DEFAULT_BASE_DELAY)

        assertFalse(attempts.isLocked())
        assertEquals(Duration.ZERO, attempts.remainingLock())
    }

    private class TestClock(
        private var current: Instant = Instant.parse("2026-10-22T10:00:00Z"),
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = current

        fun advance(duration: Duration) {
            current = current.plus(duration)
        }
    }
}
