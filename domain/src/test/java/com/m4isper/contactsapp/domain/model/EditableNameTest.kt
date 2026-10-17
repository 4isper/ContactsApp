package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditableNameTest {
    @Test
    fun `composes given middle family in order`() {
        val name = EditableName(given = "Иван", middle = "Петрович", family = "Петров")

        assertEquals("Иван Петрович Петров", name.composed)
    }

    @Test
    fun `compose includes prefix and suffix and skips blanks`() {
        val name =
            EditableName(
                prefix = "г-н",
                given = "Иван",
                family = " ",
                suffix = "Jr.",
            )

        assertEquals("г-н Иван Jr.", name.composed)
    }

    @Test
    fun `compose skips everything blank`() {
        assertEquals("", EditableName(given = " ", family = "").composed)
    }

    @Test
    fun `display mode empty ignores components`() {
        val name = EditableName(mode = NameMode.DISPLAY, display = "", given = "Иван")

        assertTrue(name.isEmpty)
    }

    @Test
    fun `structured mode empty when all components blank`() {
        val empty = EditableName(mode = NameMode.STRUCTURED, given = " ", family = "")
        val filled = EditableName(mode = NameMode.STRUCTURED, family = "Петров")

        assertTrue(empty.isEmpty)
        assertFalse(filled.isEmpty)
        assertTrue(filled.hasComponents)
    }
}
