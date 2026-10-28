package com.m4isper.contactsapp.presentation.contacts.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FastScrollerGeometryTest {
    @Test
    fun `thumb sits at the top when the list is at the start`() {
        assertEquals(0f, thumbTop(progress = 0f, trackHeight = 400f, thumbHeight = 32f))
    }

    @Test
    fun `thumb sits at the bottom when the list is at the end`() {
        assertEquals(368f, thumbTop(progress = 1f, trackHeight = 400f, thumbHeight = 32f))
    }

    @Test
    fun `thumb follows the middle of the track`() {
        assertEquals(184f, thumbTop(progress = 0.5f, trackHeight = 400f, thumbHeight = 32f))
    }

    @Test
    fun `thumb is clamped to the track`() {
        assertEquals(0f, thumbTop(progress = -1f, trackHeight = 400f, thumbHeight = 32f))
        assertEquals(368f, thumbTop(progress = 2f, trackHeight = 400f, thumbHeight = 32f))
    }

    @Test
    fun `touch maps to the letter under the finger`() {
        assertEquals(0, letterIndexAt(y = 0f, height = 300, size = 3))
        assertEquals(1, letterIndexAt(y = 150f, height = 300, size = 3))
        assertEquals(2, letterIndexAt(y = 299f, height = 300, size = 3))
    }

    @Test
    fun `touch clamps outside the index and rejects empty lists`() {
        assertEquals(0, letterIndexAt(y = -20f, height = 300, size = 3))
        assertEquals(2, letterIndexAt(y = 400f, height = 300, size = 3))
        assertEquals(null, letterIndexAt(y = 100f, height = 0, size = 3))
        assertEquals(null, letterIndexAt(y = 100f, height = 300, size = 0))
    }
}
