package com.m4isper.contactsapp.domain.model.contact

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

/**
 * Индекс первой строки [field] с невалидным значением: презентация подсвечивает
 * именно её, а не каждую строку поля. Пустые значения не считаются ошибкой.
 * null — ошибок нет.
 */
fun firstInvalidRowIndex(
    form: ContactForm,
    field: FormField,
): Int? =
    when (field) {
        FormField.PHONE ->
            form.phones.indexOfFirst { it.number.isNotBlank() && Phone.of(it.number) == null }.takeIf { it >= 0 }
        FormField.EMAIL ->
            form.emails.indexOfFirst { it.address.isNotBlank() && Email.of(it.address) == null }.takeIf { it >= 0 }
        FormField.WEBSITE ->
            form.websites.indexOfFirst { it.url.isNotBlank() && Url.of(it.url) == null }.takeIf { it >= 0 }
        FormField.EVENT ->
            form.events.indexOfFirst { it.unparsedDate != null }.takeIf { it >= 0 }
    }
