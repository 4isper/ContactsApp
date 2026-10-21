package com.m4isper.contactsapp.presentation.import

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.vcard.VCardContact
import java.util.Locale

/** Минимальная длина номера (в цифрах): короче — совпадение считаем случайным. */
private const val MIN_PHONE_DIGITS = 5

/**
 * Похожие контакты для контакта из файла: совпадение по номеру телефона или адресу почты.
 * Только их предлагают влить в существующий контакт вместо создания дубля.
 */
internal fun findImportDuplicates(
    contact: VCardContact,
    contacts: List<ContactDetails>,
): List<ContactDetails> {
    val phones =
        contact.form.phones
            .map { it.number.digits() }
            .filter { it.length >= MIN_PHONE_DIGITS }
            .toSet()
    val emails =
        contact.form.emails
            .map { it.address.normalized() }
            .filter { it.isNotEmpty() }
            .toSet()
    if (phones.isEmpty() && emails.isEmpty()) return emptyList()
    return contacts.filter { existing -> existing.matches(phones, emails) }
}

private fun ContactDetails.matches(
    phones: Set<String>,
    emails: Set<String>,
): Boolean {
    val existingPhones = phones.map { it.digits() }.toSet()
    val existingEmails = emails.map { it.normalized() }.toSet()
    return existingPhones.any { it in phones } || existingEmails.any { it in emails }
}

private fun String.digits(): String = filter(Char::isDigit)

private fun String.normalized(): String = trim().lowercase(Locale.ROOT)
