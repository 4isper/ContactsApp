package com.m4isper.contactsapp.data.datasource.write.contacts

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsPhotoProcessorFullTest {
    @Test
    fun compressPhoto_returnsJpegBytes() {
        val bitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        val bytes =
            java.io.ByteArrayOutputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        bitmap.recycle()
        val result = compressPhoto(bytes)
        assertTrue(result.isNotEmpty())
    }
}
