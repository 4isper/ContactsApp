package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.vcard.VCardCodec

/**
 * Фейк порта vCard: презентационные тесты не зависят от формата .vcf —
 * разбор отдаёт заранее заданные формы, сборка возвращает заданный текст,
 * а вызовы записываются для проверок.
 */
internal class FakeVCardCodec(
    var forms: List<ContactForm> = emptyList(),
    var encoded: String = "VCARD",
) : VCardCodec {
    val parsedTexts = mutableListOf<String>()
    val encodedContacts = mutableListOf<List<ContactDetails>>()

    override suspend fun parse(text: String): List<ContactForm> {
        parsedTexts += text
        return forms
    }

    override suspend fun encode(contacts: List<ContactDetails>): String {
        encodedContacts += contacts
        return encoded
    }
}
