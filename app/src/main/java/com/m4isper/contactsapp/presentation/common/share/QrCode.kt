package com.m4isper.contactsapp.presentation.common.share

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.m4isper.contactsapp.presentation.common.logw

private const val QR_TAG = "ContactsQr"

/**
 * QR-код с содержимым: в него кладут vCard контакта, чтобы другой телефон
 * отсканировал карточку обычной камерой.
 */
internal fun qrCodeBitmap(
    content: String,
    sizePx: Int = QR_SIZE_PX,
): Bitmap? {
    val matrix = qrMatrix(content, sizePx) ?: return null
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        val offset = y * sizePx
        for (x in 0 until sizePx) {
            pixels[offset + x] = if (matrix[y * sizePx + x]) Color.BLACK else Color.WHITE
        }
    }
    return createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    }
}

/**
 * Точки QR-кода: true — тёмная точка. Разбор не зависит от Android,
 * поэтому проверяется обычным тестом.
 */
internal fun qrMatrix(
    content: String,
    sizePx: Int = QR_SIZE_PX,
): BooleanArray? =
    try {
        val matrix: BitMatrix =
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
            )
        BooleanArray(sizePx * sizePx) { index ->
            matrix[index % sizePx, index / sizePx]
        }
    } catch (e: WriterException) {
        logw(QR_TAG, "build qr code failed", e)
        null
    } catch (e: IllegalArgumentException) {
        // Пустое содержимое или слишком длинная строка: QR-код построить нельзя.
        logw(QR_TAG, "invalid qr content", e)
        null
    }

/** Размер QR-кода в пикселях: достаточно, чтобы камера уверенно считывала. */
private const val QR_SIZE_PX = 512
