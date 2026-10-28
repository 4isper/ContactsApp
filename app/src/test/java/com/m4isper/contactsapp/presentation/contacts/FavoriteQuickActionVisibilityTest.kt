package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.presentation.contacts.components.favoriteQuickActionVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteQuickActionVisibilityTest {
    @Test
    fun `visible on a 411 dp phone at normal font scale`() {
        assertTrue(favoriteQuickActionVisible(screenWidthDp = 411, fontScale = 1f))
    }

    @Test
    fun `collapses on a 360 dp phone where the name budget is short`() {
        // 360 − 228 = 132 dp < 136: имя под звездой ужмётся сильнее допустимого.
        assertFalse(favoriteQuickActionVisible(screenWidthDp = 360, fontScale = 1f))
    }

    @Test
    fun `collapses at 320 dp and 2x font scale`() {
        assertFalse(favoriteQuickActionVisible(screenWidthDp = 320, fontScale = 2f))
    }

    @Test
    fun `moderate font scale keeps the quick action on a wide screen`() {
        assertTrue(favoriteQuickActionVisible(screenWidthDp = 852, fontScale = 2f))
    }

    @Test
    fun `large font scale collapses the quick action even on wide screens`() {
        assertFalse(favoriteQuickActionVisible(screenWidthDp = 852, fontScale = 5f))
    }

    @Test
    fun `scroller column is part of the row chrome`() {
        // 228 включает 48 dp колонки скроллера: без неё на 360 видимая ширина имени
        // была бы 180, а с ней — 132.
        assertFalse(favoriteQuickActionVisible(screenWidthDp = 228 + 135, fontScale = 1f))
        assertTrue(favoriteQuickActionVisible(screenWidthDp = 228 + 136, fontScale = 1f))
    }
}
