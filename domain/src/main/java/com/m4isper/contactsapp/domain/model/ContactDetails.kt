package com.m4isper.contactsapp.domain.model

import java.util.Locale

enum class PhoneKind {
    MOBILE,
    HOME,
    WORK,
    OTHER,
}

enum class EmailKind {
    HOME,
    WORK,
    OTHER,
}

enum class AddressKind {
    HOME,
    WORK,
    OTHER,
}

data class PhoneNumber(
    val number: String,
    val kind: PhoneKind,
    val customLabel: String? = null,
)

data class EmailAddress(
    val address: String,
    val kind: EmailKind,
    val customLabel: String? = null,
)

data class PostalAddress(
    val address: String,
    val kind: AddressKind,
    val customLabel: String? = null,
)

data class Website(
    val url: String,
)

data class Organization(
    val company: String,
    val title: String,
)

/** День рождения: год может отсутствовать (формат "--MM-dd" в ContactsContract). */
data class Birthday(
    val day: Int,
    val month: Int,
    val year: Int? = null,
) {
    fun toStorageString(): String =
        if (year == null) {
            String.format(Locale.US, "--%02d-%02d", month, day)
        } else {
            String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        }

    companion object {
        private val PATTERN = Regex("""(?:(\d{4})-|--)(\d{2})-(\d{2})""")
        private val MONTHS = 1..12
        private val DAYS = 1..31
        private const val YEAR_GROUP = 1
        private const val MONTH_GROUP = 2
        private const val DAY_GROUP = 3

        /** Разбирает "yyyy-MM-dd" или "--MM-dd"; null, если формат не распознан. */
        fun parse(raw: String?): Birthday? =
            PATTERN
                .matchEntire(raw?.trim().orEmpty())
                ?.let(::birthdayFromMatch)

        private fun birthdayFromMatch(match: MatchResult): Birthday? =
            match.groupValues[MONTH_GROUP]
                .toIntOrNull()
                ?.takeIf { it in MONTHS }
                ?.let { month ->
                    match.groupValues[DAY_GROUP]
                        .toIntOrNull()
                        ?.takeIf { it in DAYS }
                        ?.let { day ->
                            Birthday(
                                day = day,
                                month = month,
                                year = match.groupValues[YEAR_GROUP].takeIf { it.isNotEmpty() }?.toIntOrNull(),
                            )
                        }
                }
    }
}

/** Компоненты структурированного имени контакта (StructuredName). */
data class NameParts(
    val given: String = "",
    val family: String = "",
    val middle: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val phoneticGiven: String = "",
    val phoneticMiddle: String = "",
    val phoneticFamily: String = "",
) {
    val isEmpty: Boolean
        get() =
            given.isBlank() &&
                family.isBlank() &&
                middle.isBlank() &&
                prefix.isBlank() &&
                suffix.isBlank() &&
                phoneticGiven.isBlank() &&
                phoneticMiddle.isBlank() &&
                phoneticFamily.isBlank()
}

data class ContactDetails(
    val contactId: Long,
    val name: String,
    val phones: List<PhoneNumber> = emptyList(),
    val emails: List<EmailAddress> = emptyList(),
    val addresses: List<PostalAddress> = emptyList(),
    val websites: List<Website> = emptyList(),
    val organization: Organization? = null,
    val note: String? = null,
    val birthday: Birthday? = null,
    val photoUri: String? = null,
    val nameParts: NameParts? = null,
) {
    val primaryPhone: String
        get() = phones.firstOrNull()?.number.orEmpty()

    val phoneCount: Int
        get() = phones.size

    val emailCount: Int
        get() = emails.size
}
