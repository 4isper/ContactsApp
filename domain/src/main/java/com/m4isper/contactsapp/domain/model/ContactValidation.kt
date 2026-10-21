package com.m4isper.contactsapp.domain.model

/**
 * Проверка отдельных полей формы: возвращает ошибку по каждому проблемному полю.
 * Пустая форма — отдельный случай ([FormError.EMPTY_FORM]), её проверяет [validateContactForm].
 */
fun validateContactFields(form: ContactForm): Map<FormField, FormError> {
    val errors = mutableMapOf<FormField, FormError>()
    if (form.phones.any { it.number.isNotBlank() && !it.number.isValidPhone() }) {
        errors[FormField.PHONE] = FormError.INVALID_PHONE
    }
    if (form.emails.any { it.address.isNotBlank() && !it.address.isValidEmail() }) {
        errors[FormField.EMAIL] = FormError.INVALID_EMAIL
    }
    if (form.websites.any { it.url.isNotBlank() && !it.url.isValidWebsite() }) {
        errors[FormField.WEBSITE] = FormError.INVALID_URL
    }
    if (form.events.any { it.unparsedDate != null }) {
        errors[FormField.EVENT] = FormError.INVALID_DATE
    }
    return errors
}

/** Телефон: хотя бы несколько цифр и никаких пробелов внутри. */
private fun String.isValidPhone(): Boolean {
    if (any { it.isWhitespace() }) return false
    return count { it.isDigit() } >= MIN_PHONE_DIGITS
}

/** Почта: есть «@», домен с точкой и нет пробелов. */
private fun String.isValidEmail(): Boolean {
    val parts = split('@')
    val domain = parts.getOrNull(1).orEmpty()
    return parts.size == 2 &&
        none { it.isWhitespace() } &&
        domain.contains('.') &&
        !domain.startsWith('.') &&
        !domain.endsWith('.')
}

/** Сайт: либо схема, либо домен с точкой; пробелов быть не должно. */
private fun String.isValidWebsite(): Boolean {
    if (any { it.isWhitespace() }) return false
    val host = substringAfter("://", substringBefore("://"))
    return host.contains('.') && !host.startsWith('.') && !host.endsWith('.')
}

/** Минимальное число цифр в телефоне. */
private const val MIN_PHONE_DIGITS = 3
