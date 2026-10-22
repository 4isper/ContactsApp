package com.m4isper.contactsapp.data.datasource.read

/** Существующая строка номера: нужна для diff при обновлении. */
data class ExistingPhone(
    val dataId: Long,
    val rawContactId: Long,
    val number: String,
    val type: Int,
    val label: String?,
    val isPrimary: Boolean = false,
)

data class ExistingEmail(
    val dataId: Long,
    val rawContactId: Long,
    val address: String,
    val type: Int,
    val label: String?,
    val isPrimary: Boolean = false,
)

/** Адрес: формула в data1, структурированные части — в data4..data10. */
data class ExistingAddress(
    val dataId: Long,
    val rawContactId: Long,
    val address: String,
    val type: Int,
    val label: String?,
    val street: String = "",
    val poBox: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val region: String = "",
    val postcode: String = "",
    val country: String = "",
)

data class ExistingWebsite(
    val dataId: Long,
    val rawContactId: Long,
    val url: String,
    val type: Int = 0,
    val label: String? = null,
)

/** Существующая строка SIP-адреса: значение в data1, тип в data2, подпись в data3. */
data class ExistingSipAddress(
    val dataId: Long,
    val rawContactId: Long,
    val address: String,
    val type: Int,
    val label: String?,
)

/** Существующая строка мессенджера: протокол в data5, произвольный сервис в data6. */
data class ExistingInstantMessage(
    val dataId: Long,
    val rawContactId: Long,
    val address: String,
    val type: Int,
    val protocol: Int?,
    val customProtocol: String?,
)

data class ExistingOrganization(
    val dataId: Long,
    val rawContactId: Long,
    val company: String,
    val title: String,
    val department: String = "",
    val jobDescription: String = "",
    val officeLocation: String = "",
)

/** Существующая строка связи: имя в data1, тип в data2, подпись в data3. */
data class ExistingRelation(
    val dataId: Long,
    val rawContactId: Long,
    val name: String,
    val type: Int,
    val label: String?,
)

/** Существующая строка события (кроме дня рождения): дата в data1, тип в data2. */
data class ExistingEvent(
    val dataId: Long,
    val rawContactId: Long,
    val raw: String,
    val type: Int,
    val label: String?,
)

data class ExistingNickname(
    val dataId: Long,
    val rawContactId: Long,
    val nickname: String,
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

/** Существующая строка членства в группе: id группы в data1. */
data class ExistingGroupMembership(
    val dataId: Long,
    val rawContactId: Long,
    val groupId: Long,
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
    val instantMessages: List<ExistingInstantMessage> = emptyList(),
    val sipAddresses: List<ExistingSipAddress> = emptyList(),
    val relations: List<ExistingRelation> = emptyList(),
    val events: List<ExistingEvent> = emptyList(),
    val organization: ExistingOrganization? = null,
    val note: ExistingNote? = null,
    val birthday: ExistingBirthday? = null,
    val nickname: ExistingNickname? = null,
    val groupMemberships: List<ExistingGroupMembership> = emptyList(),
)
