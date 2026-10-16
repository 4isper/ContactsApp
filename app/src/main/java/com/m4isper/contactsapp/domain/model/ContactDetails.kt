package com.m4isper.contactsapp.domain.model

enum class PhoneKind {
    MOBILE,
    HOME,
    WORK,
    OTHER,
}

data class PhoneNumber(
    val number: String,
    val kind: PhoneKind,
    val customLabel: String? = null,
)

data class ContactDetails(
    val contactId: Long,
    val name: String,
    val phones: List<PhoneNumber>,
    val photoUri: String? = null,
) {
    val primaryPhone: String
        get() = phones.firstOrNull()?.number.orEmpty()

    val phoneCount: Int
        get() = phones.size
}
