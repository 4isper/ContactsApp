package com.m4isper.contactsapp.data.datasource

/** Существующая строка номера: нужна для diff при обновлении. */
data class ExistingPhone(
    val dataId: Long,
    val rawContactId: Long,
    val number: String,
    val type: Int,
    val label: String?,
)

data class ExistingEmail(
    val dataId: Long,
    val rawContactId: Long,
    val address: String,
    val type: Int,
    val label: String?,
)

/** Адрес хранится одной строкой: читаем formatted-значение (data1). */
data class ExistingAddress(
    val dataId: Long,
    val rawContactId: Long,
    val address: String,
    val type: Int,
    val label: String?,
)

data class ExistingWebsite(
    val dataId: Long,
    val rawContactId: Long,
    val url: String,
)

data class ExistingOrganization(
    val dataId: Long,
    val rawContactId: Long,
    val company: String,
    val title: String,
)

data class ExistingNote(
    val dataId: Long,
    val rawContactId: Long,
    val note: String,
)

/** Сырое значение дня рождения в формате "yyyy-MM-dd" или "--MM-dd". */
data class ExistingBirthday(
    val dataId: Long,
    val rawContactId: Long,
    val raw: String,
)

/** Существующая строка структурированного имени: нужна для diff при обновлении. */
data class ExistingName(
    val dataId: Long,
    val rawContactId: Long,
    val display: String,
    val given: String,
    val family: String,
    val middle: String,
    val prefix: String,
    val suffix: String,
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
}

/** Сырые строки контакта: всё, что нужно для diff при обновлении. */
data class ContactRows(
    val names: List<ExistingName> = emptyList(),
    val phones: List<ExistingPhone> = emptyList(),
    val emails: List<ExistingEmail> = emptyList(),
    val addresses: List<ExistingAddress> = emptyList(),
    val websites: List<ExistingWebsite> = emptyList(),
    val organization: ExistingOrganization? = null,
    val note: ExistingNote? = null,
    val birthday: ExistingBirthday? = null,
)
