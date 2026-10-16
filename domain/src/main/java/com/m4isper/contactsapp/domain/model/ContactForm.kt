package com.m4isper.contactsapp.domain.model

data class EditablePhone(
    val dataId: Long? = null,
    val number: String,
    val kind: PhoneKind = PhoneKind.MOBILE,
    val customLabel: String? = null,
)

data class EditableEmail(
    val dataId: Long? = null,
    val address: String,
    val kind: EmailKind = EmailKind.HOME,
    val customLabel: String? = null,
)

data class EditableAddress(
    val dataId: Long? = null,
    val address: String,
    val kind: AddressKind = AddressKind.HOME,
    val customLabel: String? = null,
)

data class EditableWebsite(
    val dataId: Long? = null,
    val url: String,
)

data class EditableOrganization(
    val dataId: Long? = null,
    val company: String = "",
    val title: String = "",
) {
    val isEmpty: Boolean
        get() = company.isBlank() && title.isBlank()
}

data class EditableNote(
    val dataId: Long? = null,
    val note: String = "",
)

data class EditableBirthday(
    val dataId: Long? = null,
    val value: Birthday? = null,
)

data class ContactForm(
    val name: String,
    val phones: List<EditablePhone> = emptyList(),
    val emails: List<EditableEmail> = emptyList(),
    val addresses: List<EditableAddress> = emptyList(),
    val websites: List<EditableWebsite> = emptyList(),
    val organization: EditableOrganization = EditableOrganization(),
    val note: EditableNote = EditableNote(),
    val birthday: EditableBirthday = EditableBirthday(),
    /** Текущее фото (thumbnail существующего контакта или выбранный пользователем URI). */
    val photoUri: String? = null,
    /** true, если пользователь сменил или удалил фото — только тогда фото трогаем. */
    val photoChanged: Boolean = false,
)

enum class FormError {
    EMPTY_FORM,
    BLANK_PHONE,
}

fun validateContactForm(form: ContactForm): FormError? =
    when {
        form.isEffectivelyEmpty() -> FormError.EMPTY_FORM
        form.phones.any { it.number.isBlank() } -> FormError.BLANK_PHONE
        else -> null
    }

private fun ContactForm.isEffectivelyEmpty(): Boolean =
    name.isBlank() &&
        phones.none { it.number.isNotBlank() } &&
        emails.none { it.address.isNotBlank() } &&
        addresses.none { it.address.isNotBlank() } &&
        websites.none { it.url.isNotBlank() } &&
        organization.isEmpty &&
        note.note.isBlank() &&
        birthday.value == null
