package com.m4isper.contactsapp.domain.model

/** Минимальная длина номера (в цифрах): короче — совпадение считаем случайным. */
private const val MIN_PHONE_DIGITS = 5

/**
 * Контакты, похожие на заполняемую форму: совпадает номер телефона или адрес почты.
 * Нужны, чтобы предупредить о вероятном дубле до сохранения.
 */
fun findSimilarContacts(
    form: ContactForm,
    contacts: List<ContactDetails>,
): List<ContactDetails> {
    val phones =
        form.phones
            .map { it.number.digits() }
            .filter { it.length >= MIN_PHONE_DIGITS }
            .toSet()
    val emails =
        form.emails
            .map { it.address.normalized() }
            .filter { it.isNotEmpty() }
            .toSet()
    if (phones.isEmpty() && emails.isEmpty()) return emptyList()
    return contacts.filter { contact ->
        contact.phones.any { it.number.digits() in phones } ||
            contact.emails.any { it.address.normalized() in emails }
    }
}

private fun String.digits(): String = filter(Char::isDigit)

private fun String.normalized(): String = trim().lowercase()
