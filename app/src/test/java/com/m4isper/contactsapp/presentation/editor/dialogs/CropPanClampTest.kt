package com.m4isper.contactsapp.presentation.editor.dialogs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class CropPanClampTest {
    private val viewport = IntSize(300, 300)
    private val bitmapSize = 600 to 600

    @Test
    fun `pan stays inside the image bounds`() {
        val pan = clampedPan(Offset(500f, 500f), Offset.Zero, bitmapSize.first, bitmapSize.second, 1f, viewport)

        assertEquals(Offset(150f, 150f), pan)
    }

    @Test
    fun `zoom out re-clamps the old pan immediately`() {
        // Картинка вписана ровно в канву: любое смещение недопустимо, даже оставшееся от прошлого зума.
        val pan = clampedPan(Offset(150f, 150f), Offset.Zero, bitmapSize.first, bitmapSize.second, 0.5f, viewport)

        assertEquals(Offset.Zero, pan)
    }

    @Test
    fun `drag applies and clamps with the new scale`() {
        val pan = clampedPan(Offset(0f, 0f), Offset(-400f, 0f), bitmapSize.first, bitmapSize.second, 2f, viewport)

        assertEquals(Offset(-400f, 0f), pan)
    }
}
