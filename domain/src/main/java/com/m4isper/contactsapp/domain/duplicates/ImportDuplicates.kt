package com.m4isper.contactsapp.domain.duplicates

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.normalizeContactText
import com.m4isper.contactsapp.domain.model.normalizePhoneDigits
import com.m4isper.contactsapp.domain.vcard.VCardContact

/** Минимальная длина номера (в цифрах): короче — совпадение считаем случайным. */
private const val MIN_PHONE_DIGITS = 5

/**
 * Похожие контакты для контакта из файла: совпадение по номеру телефона или адресу почты.
 * Только их предлагают влить в существующий контакт вместо создания дубля.
 */
fun findImportDuplicates(
    contact: VCardContact,
    contacts: List<ContactDetails>,
): List<ContactDetails> {
    val phones =
        contact.form.phones
            .map { normalizePhoneDigits(it.number) }
            .filter { it.length >= MIN_PHONE_DIGITS }
            .toSet()
    val emails =
        contact.form.emails
            .map { normalizeContactText(it.address) }
            .filter { it.isNotEmpty() }
            .toSet()
    if (phones.isEmpty() && emails.isEmpty()) return emptyList()
    return contacts.filter { existing -> existing.matches(phones, emails) }
}

private fun ContactDetails.matches(
    phones: Set<String>,
    emails: Set<String>,
): Boolean {
    val existingPhones = phones.map { normalizePhoneDigits(it) }.toSet()
    val existingEmails = emails.map { normalizeContactText(it) }.toSet()
    return existingPhones.any { it in phones } || existingEmails.any { it in emails }
}
