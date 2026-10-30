package com.m4isper.contactsapp.presentation.contacts.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FastScrollerGeometryTest {
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
