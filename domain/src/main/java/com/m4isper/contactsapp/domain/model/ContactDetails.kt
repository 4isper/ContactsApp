package com.m4isper.contactsapp.domain.model

enum class PhoneKind {
    MOBILE,
    HOME,
    WORK,
    WORK_MOBILE,
    FAX,
    PAGER,
    CALLBACK,
    COMPANY_MAIN,
    OTHER,
}

enum class EmailKind {
    HOME,
    WORK,
    MOBILE,
    OTHER,
}

enum class AddressKind {
    HOME,
    WORK,
    OTHER,
}

enum class WebsiteKind {
    HOMEPAGE,
    HOME,
    WORK,
    BLOG,
    PROFILE,
    FTP,
    OTHER,
}

enum class SipKind {
    HOME,
    WORK,
    OTHER,
}

data class PhoneNumber(
    val number: String,
    val kind: PhoneKind,
    val customLabel: String? = null,
    val isPrimary: Boolean = false,
)

data class EmailAddress(
    val address: String,
    val kind: EmailKind,
    val customLabel: String? = null,
    val isPrimary: Boolean = false,
)

/**
 * Почтовый адрес в структурированном виде. [address] — legacy-представление
 * одной строкой, которое сохраняется для старых контактов и как fallback.
 */
data class PostalAddress(
    val address: String,
    val kind: AddressKind,
    val customLabel: String? = null,
    val street: String = "",
    val poBox: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val region: String = "",
    val postcode: String = "",
    val country: String = "",
) {
    /** Улица с fallback на старое однострочное значение. */
    val streetLine: String
        get() = street.ifBlank { address }

    val hasStructuredParts: Boolean
        get() =
            street.isNotBlank() ||
                poBox.isNotBlank() ||
                neighborhood.isNotBlank() ||
                city.isNotBlank() ||
                region.isNotBlank() ||
                postcode.isNotBlank() ||
                country.isNotBlank()

    /** Единая строка для отображения, поиска и карт. */
    val displayAddress: String
        get() = if (hasStructuredParts) composePostalAddress(this) else address
}

data class Website(
    val url: String,
    val kind: WebsiteKind = WebsiteKind.HOMEPAGE,
    val customLabel: String? = null,
)

data class SipAddress(
    val address: String,
    val kind: SipKind = SipKind.OTHER,
    val customLabel: String? = null,
)

/** Склейка непустых частей структурированного адреса в читаемую строку. */
fun composePostalAddress(address: PostalAddress): String =
    listOf(
        address.poBox,
        address.streetLine,
        address.neighborhood,
        address.city,
        address.region,
        address.postcode,
        address.country,
    ).map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

/** Протокол мессенджера: набор из ContactsContract.CommonDataKinds.Im + произвольный. */
enum class ImProtocol {
    AIM,
    MSN,
    YAHOO,
    SKYPE,
    QQ,
    GOOGLE_TALK,
    ICQ,
    JABBER,
    CUSTOM,
}

data class InstantMessage(
    val address: String,
    val protocol: ImProtocol,
    val customProtocol: String? = null,
)

/** Родственная связь: набор из ContactsContract.CommonDataKinds.Relation + произвольная. */
enum class RelationKind {
    ASSISTANT,
    BROTHER,
    CHILD,
    DOMESTIC_PARTNER,
    FATHER,
    FRIEND,
    MANAGER,
    MOTHER,
    PARENT,
    PARTNER,
    REFERRED_BY,
    RELATIVE,
    SISTER,
    SPOUSE,
    OTHER,
}

data class ContactRelation(
    val name: String,
    val kind: RelationKind,
    val customLabel: String? = null,
)

data class Organization(
    val company: String,
    val title: String,
    val department: String = "",
    val jobDescription: String = "",
    val officeLocation: String = "",
)

/** День рождения: год может отсутствовать (формат "--MM-dd" в ContactsContract). */
data class Birthday(
    val day: Int,
    val month: Int,
    val year: Int? = null,
)

/** Тип события с датой: годовщина или произвольная дата. */
enum class EventKind {
    ANNIVERSARY,
    OTHER,
}

/** Событие с датой: день/месяц/год — та же модель, что у дня рождения. */
data class ContactEvent(
    val date: Birthday,
    val kind: EventKind = EventKind.ANNIVERSARY,
    val customLabel: String? = null,
)

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
    val instantMessages: List<InstantMessage> = emptyList(),
    val sipAddresses: List<SipAddress> = emptyList(),
    val organization: Organization? = null,
    val note: String? = null,
    val birthday: Birthday? = null,
    val relations: List<ContactRelation> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val nickname: String? = null,
    val photoUri: String? = null,
    val nameParts: NameParts? = null,
    /** Аккаунт целевого raw contact; локальный контакт — [ContactAccount.Local]. */
    val account: ContactAccount = ContactAccount.Local,
    /** Время последнего изменения контакта в провайдере, мс; null, если провайдер не сообщил. */
    val lastUpdated: Long? = null,
    /** Время последнего контакта с этим человеком по данным устройства, мс; null, если его не было. */
    val lastContacted: Long? = null,
    /** Сколько раз контакт был использован для звонка/сообщения по данным устройства. */
    val timesContacted: Int = 0,
) {
    /** Основной телефон: явно помеченный или первый доступный. */
    val primaryPhone: PhoneNumber?
        get() = phones.firstOrNull { it.isPrimary } ?: phones.firstOrNull()

    /** Основной email: явно помеченный или первый доступный. */
    val primaryEmail: EmailAddress?
        get() = emails.firstOrNull { it.isPrimary } ?: emails.firstOrNull()
}
