package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.presentation.contacts.components.favoriteQuickActionVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteQuickActionVisibilityTest {
    @Test
    fun `visible on a normal phone at normal font scale`() {
        assertTrue(favoriteQuickActionVisible(screenWidthDp = 360, fontScale = 1f))
    }

    @Test
    fun `collapses when the scaled name budget cannot be met`() {
        // 320 dp минус хром строки оставляет ~116 dp — меньше 136 * 1.
        assertFalse(favoriteQuickActionVisible(screenWidthDp = 320, fontScale = 1f))
    }

    @Test
    fun `large font scale collapses the quick action even on wide screens`() {
        assertFalse(favoriteQuickActionVisible(screenWidthDp = 852, fontScale = 5f))
    }

    @Test
    fun `moderate font scale keeps the quick action on a wide screen`() {
        assertTrue(favoriteQuickActionVisible(screenWidthDp = 852, fontScale = 2f))
    }
}
