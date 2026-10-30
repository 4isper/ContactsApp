package com.m4isper.contactsapp.presentation.common.share

/** Содержимое отсканированного QR: контакт можно отправить в предпросмотр импорта. */
sealed interface QrScanContent {
    /** QR с vCard: текст уходит в предпросмотр импорта как есть. */
    data class VCard(
        val text: String,
    ) : QrScanContent

    /** QR без контакта: ссылка, обычный текст, MECARD. */
    data object NotContact : QrScanContent
}

/**
 * Решение «QR несёт контакт или нет». Маркера BEGIN:VCARD достаточно:
 * обрезанный или частично битый vCard всё равно открывает предпросмотр —
 * невалидные записи там показываются сами.
 */
internal fun qrScanContent(raw: String?): QrScanContent {
    val text = raw?.trim().orEmpty()
    return if (text.contains(VCARD_MARKER, ignoreCase = true)) {
        QrScanContent.VCard(text)
    } else {
        QrScanContent.NotContact
    }
}

private const val VCARD_MARKER = "BEGIN:VCARD"
