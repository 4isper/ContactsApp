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

data class EditableInstantMessage(
    val dataId: Long? = null,
    val address: String,
    val protocol: ImProtocol = ImProtocol.CUSTOM,
    val customProtocol: String? = null,
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

enum class NameMode {
    /** Одно поле «Полное имя»: пишется в DISPLAY_NAME. */
    DISPLAY,

    /** Компоненты Имя/Фамилия/Отчество/... : провайдер сам собирает DISPLAY_NAME. */
    STRUCTURED,
}

/**
 * Имя в редакторе: единое поле или компоненты. Собранное имя —
 * «приставка имя отчество фамилия суффикс», пустые части пропускаются.
 */
data class EditableName(
    val mode: NameMode = NameMode.DISPLAY,
    val display: String = "",
    val given: String = "",
    val family: String = "",
    val middle: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val phoneticGiven: String = "",
    val phoneticMiddle: String = "",
    val phoneticFamily: String = "",
) {
    val hasComponents: Boolean
        get() =
            given.isNotBlank() ||
                family.isNotBlank() ||
                middle.isNotBlank() ||
                prefix.isNotBlank() ||
                suffix.isNotBlank() ||
                phoneticGiven.isNotBlank() ||
                phoneticMiddle.isNotBlank() ||
                phoneticFamily.isNotBlank()

    /** Пусто ли имя в текущем режиме. */
    val isEmpty: Boolean
        get() = if (mode == NameMode.DISPLAY) display.isBlank() else !hasComponents

    val composed: String
        get() = composeDisplayName(prefix, given, middle, family, suffix)
}

/** Склейка частей имени через пробел; пустые и пробельные части пропускаются. */
fun composeDisplayName(vararg parts: String): String =
    parts
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")

data class ContactForm(
    val name: EditableName = EditableName(),
    val phones: List<EditablePhone> = emptyList(),
    val emails: List<EditableEmail> = emptyList(),
    val addresses: List<EditableAddress> = emptyList(),
    val websites: List<EditableWebsite> = emptyList(),
    val instantMessages: List<EditableInstantMessage> = emptyList(),
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
    name.isEmpty &&
        phones.none { it.number.isNotBlank() } &&
        emails.none { it.address.isNotBlank() } &&
        addresses.none { it.address.isNotBlank() } &&
        websites.none { it.url.isNotBlank() } &&
        instantMessages.none { it.address.isNotBlank() } &&
        organization.isEmpty &&
        note.note.isBlank() &&
        birthday.value == null
