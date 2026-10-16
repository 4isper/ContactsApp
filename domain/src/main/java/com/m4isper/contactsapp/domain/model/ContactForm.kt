package com.m4isper.contactsapp.domain.model

data class EditablePhone(
    val dataId: Long? = null,
    val number: String,
    val kind: PhoneKind = PhoneKind.MOBILE,
    val customLabel: String? = null,
)

data class ContactForm(
    val name: String,
    val phones: List<EditablePhone> = emptyList(),
)

enum class FormError {
    EMPTY_FORM,
    BLANK_PHONE,
}

fun validateContactForm(form: ContactForm): FormError? =
    when {
        form.name.isBlank() && form.phones.all { it.number.isBlank() } -> FormError.EMPTY_FORM
        form.phones.any { it.number.isBlank() } -> FormError.BLANK_PHONE
        else -> null
    }
