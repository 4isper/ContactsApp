package com.m4isper.contactsapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkeletonAlphaTest {
    @Test
    fun `alpha spans the subtle range`() {
        assertEquals(SKELETON_ALPHA_MIN, skeletonAlpha(0f), 0.001f)
        assertEquals(1f, skeletonAlpha(1f), 0.001f)
    }

    @Test
    fun `alpha grows monotonically with progress`() {
        assertTrue(skeletonAlpha(0.7f) > skeletonAlpha(0.3f))
    }
}
