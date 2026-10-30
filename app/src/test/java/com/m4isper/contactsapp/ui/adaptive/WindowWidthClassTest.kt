package com.m4isper.contactsapp.ui.adaptive

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowWidthClassTest {
    @Test
    fun `narrow window is compact`() {
        assertEquals(WindowWidthClass.COMPACT, widthClassOf(399f))
    }

    @Test
    fun `medium window starts at 600 dp`() {
        assertEquals(WindowWidthClass.MEDIUM, widthClassOf(600f))
        assertEquals(WindowWidthClass.MEDIUM, widthClassOf(839f))
    }

    @Test
    fun `expanded window starts at 840 dp`() {
        assertEquals(WindowWidthClass.EXPANDED, widthClassOf(840f))
        assertEquals(WindowWidthClass.EXPANDED, widthClassOf(1280f))
    }

    private fun widthClassOf(widthDp: Float): WindowWidthClass =
        windowWidthClassOf(
            WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(widthDp = widthDp, heightDp = 800f),
        )
}
