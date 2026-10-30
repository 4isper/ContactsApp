package com.m4isper.contactsapp.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Два шага установки PIN: первый — выбор кода (длину определяет пользователь,
 * поэтому отправка по клавише), второй — повтор с проверкой совпадения.
 */
class SetupPinEntryTest {
    @Test
    fun `a short first entry stays on the first step and flags the hint`() {
        val entry = SetupPinEntry()
        repeat(3) { entry.append(1) }

        assertFalse(entry.confirmFirst())

        assertEquals(SetupStage.FIRST, entry.stage)
        assertTrue(entry.shortFlash)
    }

    @Test
    fun `confirm first moves to the repeat step with a clean entry`() {
        val entry = SetupPinEntry()
        repeat(4) { entry.append(2) }

        assertTrue(entry.confirmFirst())

        assertEquals(SetupStage.REPEAT, entry.stage)
        assertEquals("2222", entry.first)
        assertEquals("", entry.pin)
        assertFalse(entry.shortFlash)
    }

    @Test
    fun `the repeat step is done when the entry matches`() {
        val entry = SetupPinEntry()
        repeat(4) { entry.append(1) }
        entry.confirmFirst()
        repeat(4) { entry.append(1) }

        assertTrue(entry.done)
        assertFalse(entry.mismatch)
        assertTrue(entry.confirmPair())
    }

    @Test
    fun `a differing repeat flags a mismatch at full length`() {
        val entry = SetupPinEntry()
        repeat(4) { entry.append(1) }
        entry.confirmFirst()
        entry.append(1)
        entry.append(1)
        entry.append(1)
        entry.append(5)

        assertFalse(entry.done)
        assertTrue(entry.mismatch)
        assertFalse(entry.confirmPair())
    }

    @Test
    fun `a partial repeat is neither mismatch nor done`() {
        val entry = SetupPinEntry()
        repeat(6) { entry.append(7) }
        entry.confirmFirst()
        entry.append(7)

        assertFalse(entry.mismatch)
        assertFalse(entry.done)
    }

    @Test
    fun `delete clears the flash and the mismatch`() {
        val entry = SetupPinEntry()
        repeat(4) { entry.append(1) }
        entry.confirmFirst()
        repeat(3) { entry.append(1) }
        entry.append(9)

        assertTrue(entry.mismatch)

        entry.delete()

        assertFalse(entry.mismatch)
        assertFalse(entry.done)
        assertEquals("111", entry.pin)
    }

    @Test
    fun `the entry caps at the maximum length`() {
        val entry = SetupPinEntry()
        repeat(12) { index -> entry.append(index % 10) }

        assertEquals(MAX_PIN_LENGTH, entry.pin.length)
    }
}
