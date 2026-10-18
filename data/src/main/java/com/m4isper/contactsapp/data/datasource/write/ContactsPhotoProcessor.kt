package com.m4isper.contactsapp.data.datasource.write

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

private const val MAX_PHOTO_PX = 720
private const val PHOTO_QUALITY = 85

/** Читает фото из [uri], уменьшает до [MAX_PHOTO_PX] и сжимает в JPEG. */
internal fun readDownscaledPhoto(
    resolver: ContentResolver,
    uri: Uri,
): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IllegalArgumentException("Unsupported image: $uri")
    }
    val options =
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_PX)
        }
    val decoded =
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Can't read image: $uri")
    val scaled = scaleDown(decoded, MAX_PHOTO_PX)
    return try {
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out)
            out.toByteArray()
        }
    } finally {
        scaled.recycle()
        if (scaled !== decoded) decoded.recycle()
    }
}

/** Степень двойки, после которой декодированная картинка станет меньше max. */
private fun sampleSize(
    width: Int,
    height: Int,
    max: Int,
): Int {
    var sample = 1
    while (width / (sample * 2) >= max || height / (sample * 2) >= max) {
        sample *= 2
    }
    return sample
}

private fun scaleDown(
    bitmap: Bitmap,
    max: Int,
): Bitmap {
    val largest = maxOf(bitmap.width, bitmap.height)
    if (largest <= max) return bitmap
    val ratio = max.toFloat() / largest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt().coerceAtLeast(1),
        (bitmap.height * ratio).toInt().coerceAtLeast(1),
        true,
    )
}
