package com.m4isper.contactsapp.presentation.common.share

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Решение «QR несёт контакт или нет» принимает чистая функция:
 * сканер только отдаёт текст, а предпросмотр импорта получает vCard целиком.
 */
class QrScanContentTest {
    @Test
    fun vCardQrIsAContact() {
        val vCard = "BEGIN:VCARD\nVERSION:4.0\nFN:Ada\nEND:VCARD"

        assertEquals(QrScanContent.VCard(vCard), qrScanContent(vCard))
    }

    @Test
    fun vCardMarkersAreCaseInsensitive() {
        val vCard = "begin:vcard\nVERSION:4.0\nfn:Ada\nend:vcard"

        assertEquals(QrScanContent.VCard(vCard), qrScanContent(vCard))
    }

    @Test
    fun surroundingWhitespaceIsTrimmedButContentKept() {
        val vCard = "  BEGIN:VCARD\nNOTE:keeps inner spacing \nEND:VCARD  "

        assertEquals(
            QrScanContent.VCard(vCard.trim()),
            qrScanContent(vCard),
        )
    }

    @Test
    fun truncatedVCardStillOpensThePreview() {
        val truncated = "BEGIN:VCARD\nVERSION:4.0\nFN:Ada"

        assertEquals(QrScanContent.VCard(truncated.trim()), qrScanContent(truncated))
    }

    @Test
    fun urlQrIsNotAContact() {
        assertEquals(QrScanContent.NotContact, qrScanContent("https://example.com"))
    }

    @Test
    fun mecardQrIsNotAContact() {
        assertEquals(QrScanContent.NotContact, qrScanContent("MECARD:N:Ada;TEL:123;;"))
    }

    @Test
    fun emptyQrIsNotAContact() {
        assertEquals(QrScanContent.NotContact, qrScanContent(""))
    }

    @Test
    fun cancelledScanWithoutContentsIsNotAContact() {
        assertEquals(QrScanContent.NotContact, qrScanContent(null))
    }
}
