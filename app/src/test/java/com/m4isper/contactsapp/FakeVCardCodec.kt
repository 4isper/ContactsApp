package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.domain.vcard.VCardContact
import com.m4isper.contactsapp.domain.vcard.VCardExportContact
import com.m4isper.contactsapp.domain.vcard.VCardParseResult

/**
 * Фейк порта vCard: презентационные тесты не зависят от формата .vcf —
 * разбор отдаёт заранее заданные формы, сборка возвращает заданный текст,
 * а вызовы записываются для проверок.
 */
internal class FakeVCardCodec(
    var forms: List<ContactForm> = emptyList(),
    var encoded: String = "VCARD",
    var failure: Throwable? = null,
) : VCardCodec {
    val parsedTexts = mutableListOf<String>()
    val encodedContacts = mutableListOf<List<VCardExportContact>>()

    override suspend fun parse(text: String): VCardParseResult {
        parsedTexts += text
        failure?.let { throw it }
        return VCardParseResult(contacts = forms.map { VCardContact(form = it) })
    }

    override suspend fun encode(contacts: List<VCardExportContact>): String {
        encodedContacts += contacts
        return encoded
    }
}
