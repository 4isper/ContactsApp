package com.m4isper.contactsapp.presentation.editor.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DragStepsTest {
    @Test
    fun `a short drag stays within the row`() {
        assertEquals(0, dragSteps(offsetPx = 47f, thresholdPx = 48f))
    }

    @Test
    fun `each passed threshold is one row`() {
        assertEquals(1, dragSteps(offsetPx = 48f, thresholdPx = 48f))
        assertEquals(2, dragSteps(offsetPx = 120f, thresholdPx = 48f))
    }

    @Test
    fun `upward drag is negative`() {
        assertEquals(-2, dragSteps(offsetPx = -100f, thresholdPx = 48f))
    }

    @Test
    fun `a zero threshold never moves`() {
        assertEquals(0, dragSteps(offsetPx = 100f, thresholdPx = 0f))
    }
}
