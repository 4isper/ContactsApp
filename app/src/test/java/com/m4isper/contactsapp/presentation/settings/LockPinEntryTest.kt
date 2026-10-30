package com.m4isper.contactsapp.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Состояние ввода PIN: известная длина даёт сигнал автоотправки на последней
 * цифре, легаси-режим — ручную отправку с подсветкой короткого ввода.
 */
class LockPinEntryTest {
    @Test
    fun `append signals submit when the known length is reached`() {
        val entry = LockPinEntry(targetLength = 4)

        assertFalse(entry.append(1))
        assertFalse(entry.append(2))
        assertFalse(entry.append(3))
        assertTrue(entry.append(4))
        assertEquals("1234", entry.pin)
    }

    @Test
    fun `append never signals submit when the length is unknown`() {
        val entry = LockPinEntry(targetLength = null)

        repeat(8) { index -> assertFalse(entry.append(index + 1)) }

        assertEquals("12345678", entry.pin)
    }

    @Test
    fun `append caps the pin at the maximum length`() {
        val entry = LockPinEntry(targetLength = null)

        repeat(12) { index -> entry.append(index % 10) }

        assertEquals(8, entry.pin.length)
    }

    @Test
    fun `delete removes the last digit and clears the error`() {
        val entry = LockPinEntry(targetLength = 4)
        entry.append(1)
        entry.beginCheck() // короткий ввод: failed = true

        entry.delete()

        assertEquals("", entry.pin)
        assertFalse(entry.failed)
    }

    @Test
    fun `begin check flags a short pin in legacy mode without checking`() {
        val entry = LockPinEntry(targetLength = null)
        entry.append(1)
        entry.append(2)
        entry.append(3)

        assertFalse(entry.beginCheck())

        assertTrue(entry.failed)
        assertFalse(entry.checking)
    }

    @Test
    fun `begin check starts checking when the pin is long enough`() {
        val entry = LockPinEntry(targetLength = 4)
        entry.append(1)
        entry.append(2)
        entry.append(3)
        entry.append(4)

        assertTrue(entry.beginCheck())

        assertTrue(entry.checking)
    }

    @Test
    fun `finish clears the pin on failure and keeps it on success`() {
        val entry = LockPinEntry(targetLength = 4)
        entry.append(9)
        entry.append(9)
        entry.append(9)
        entry.append(9)
        entry.beginCheck()

        entry.finish(success = false)

        assertFalse(entry.checking)
        assertTrue(entry.failed)
        assertEquals("", entry.pin)

        entry.append(1)
        assertFalse(entry.failed)

        entry.finish(success = true)

        assertFalse(entry.failed)
    }
}
