package com.m4isper.contactsapp.domain.model

/**
 * Проверка отдельных полей формы: возвращает ошибку по каждому проблемному полю.
 * Пустая форма — отдельный случай ([FormError.EMPTY_FORM]), её проверяет [validateContactForm].
 */
fun validateContactFields(form: ContactForm): Map<FormField, FormError> {
    val errors = mutableMapOf<FormField, FormError>()
    if (form.phones.any { it.number.isNotBlank() && Phone.of(it.number) == null }) {
        errors[FormField.PHONE] = FormError.INVALID_PHONE
    }
    if (form.emails.any { it.address.isNotBlank() && Email.of(it.address) == null }) {
        errors[FormField.EMAIL] = FormError.INVALID_EMAIL
    }
    if (form.websites.any { it.url.isNotBlank() && Url.of(it.url) == null }) {
        errors[FormField.WEBSITE] = FormError.INVALID_URL
    }
    if (form.events.any { it.unparsedDate != null }) {
        errors[FormField.EVENT] = FormError.INVALID_DATE
    }
    return errors
}
