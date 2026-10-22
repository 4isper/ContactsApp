package com.m4isper.contactsapp.presentation.common

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Безопасный вызов: отмена пробрасывается, ошибка возвращается вызывающему. */
class SafeCallTest {
    @Test
    fun `a successful call returns the value`() {
        val result = safeCall(TAG, "message") { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `a failure is returned instead of thrown`() {
        val result = safeCall(TAG, "message") { error("boom") }

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `the cancellation of the coroutine is not swallowed`() {
        val failure =
            runCatching {
                safeCall(TAG, "message") { throw CancellationException("cancelled") }
            }.exceptionOrNull()

        assertTrue(failure is CancellationException)
    }
}

private const val TAG = "SafeCallTest"
