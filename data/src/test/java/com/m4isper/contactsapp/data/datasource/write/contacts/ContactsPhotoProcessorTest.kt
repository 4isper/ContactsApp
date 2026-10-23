package com.m4isper.contactsapp.data.datasource.write.contacts

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsPhotoProcessorTest {
    @Test
    fun compressPhoto_returnsJpegBytes() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val bytes =
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        bitmap.recycle()
        val result = compressPhoto(bytes)
        assertTrue(result.isNotEmpty())
    }
}
