package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableAddress
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditableNickname
import com.m4isper.contactsapp.domain.model.EditableNote
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.EditableSipAddress
import com.m4isper.contactsapp.domain.model.EditableWebsite
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.model.SipKind
import com.m4isper.contactsapp.domain.model.WebsiteKind
import com.m4isper.contactsapp.domain.vcard.VCardContact
import com.m4isper.contactsapp.domain.vcard.VCardInvalidContact
import com.m4isper.contactsapp.domain.vcard.VCardIssue
import com.m4isper.contactsapp.domain.vcard.VCardParseResult
import java.nio.charset.Charset
import java.util.Locale

private const val TEL_URI_PREFIX = "tel:"

/** Свойства, которые накопитель умеет читать: по ним решаем, пустой ли блок. */
private val SUPPORTED_PROPERTIES =
    setOf(
        "FN",
        "N",
        "ORG",
        "TITLE",
        "ROLE",
        "NOTE",
        "BDAY",
        "NICKNAME",
        "X-OFFICE-LOCATION",
        "TEL",
        "EMAIL",
        "ADR",
        "URL",
        "IMPP",
        "ANNIVERSARY",
        "X-ABDATE",
        "RELATED",
        "X-ABRELATEDNAMES",
        "PHOTO",
        "CATEGORIES",
    )

/**
 * Минимальный vCard-импортёр (2.1–4.0): BEGIN/END-блоки, folded-строки, экранирование,
 * групповые префиксы (item1.TEL), фото в base64, категории (CATEGORIES) и
 * значения в quoted-printable.
 */
internal fun parseVCards(text: String): VCardParseResult {
    val contacts = mutableListOf<VCardContact>()
    val invalid = mutableListOf<VCardInvalidContact>()
    var cardLines: MutableList<String>? = null

    fun flush(container: MutableList<String>?) {
        container ?: return
        when (val result = parseVCardBlock(container)) {
            is VCardBlock.Parsed -> contacts += result.contact
            is VCardBlock.Skipped -> invalid += VCardInvalidContact(result.name, result.issue)
        }
    }
    unfoldVCardLines(text.removePrefix("\uFEFF")).forEach { line ->
        val marker = line.trim().uppercase(Locale.ROOT)
        val container = cardLines
        when {
            marker == "BEGIN:VCARD" -> {
                // Незакрытый блок (битый файл) не теряем: разбираем его до нового BEGIN.
                flush(container)
                cardLines = mutableListOf()
            }
            marker == "END:VCARD" -> {
                flush(container)
                cardLines = null
            }
            container != null -> container += line
        }
    }
    flush(cardLines)
    return VCardParseResult(contacts = contacts, invalid = invalid)
}

/** Итог разбора одного блока vCard. */
private sealed interface VCardBlock {
    data class Parsed(
        val contact: VCardContact,
    ) : VCardBlock

    data class Skipped(
        val name: String,
        val issue: VCardIssue,
    ) : VCardBlock
}

private fun parseVCardBlock(lines: List<String>): VCardBlock {
    val builder = VCardAccumulator()
    lines.forEach(builder::apply)
    return builder.build()
}

/**
 * Разворачивает folded-строки: продолжение начинается с пробела или таба.
 * В quoted-printable перенос строки помечается знаком «=» в конце строки.
 */
private fun unfoldVCardLines(text: String): List<String> {
    val lines = mutableListOf<String>()
    text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .forEach { line ->
            val previous = lines.lastOrNull()
            when {
                previous != null &&
                    previous.endsWith(SOFT_BREAK) &&
                    !previous.endsWith(ESCAPED_SOFT_BREAK) &&
                    previous.isQuotedPrintable() -> {
                    // Мягкий перенос quoted-printable: «=» в конце строки убираем.
                    lines[lines.lastIndex] = previous.dropLast(1) + line
                }
                (line.startsWith(" ") || line.startsWith("\t")) && lines.isNotEmpty() -> {
                    lines[lines.lastIndex] += line.substring(1)
                }
                else -> lines += line
            }
        }
    return lines
}

/** Знак мягкого переноса quoted-printable. */
private const val SOFT_BREAK = "="

/** Строка свойства в quoted-printable: только у них «=» в конце означает перенос. */
private fun String.isQuotedPrintable(): Boolean = substringBefore(':').contains(QUOTED_PRINTABLE, ignoreCase = true)

/** Экранированный знак «=» — обычный перенос не означает. */
private const val ESCAPED_SOFT_BREAK = "\\="

private data class VCardProperty(
    val name: String,
    val params: Map<String, String>,
    val types: Set<String>,
    val value: String,
    val group: String? = null,
)

/**
 * Разбирает строку свойства: имя, параметры и значение.
 * Групповой префикс сохраняется, на него ссылаются подписи X-ABLabel.
 */
private fun parseVCardProperty(line: String): VCardProperty? {
    val colon = line.indexOf(':')
    if (colon <= 0) return null
    val rawValue = line.substring(colon + 1)
    val segments = line.substring(0, colon).split(';')
    val head = segments.first().trim()
    val name = head.substringAfterLast('.').trim().uppercase(Locale.ROOT)
    val group =
        head
            .substringBeforeLast('.', missingDelimiterValue = "")
            .trim()
            .lowercase(Locale.ROOT)
            .takeIf { it.isNotEmpty() }
    val params = mutableMapOf<String, String>()
    val types = mutableSetOf<String>()
    segments.drop(1).forEach { segment ->
        val separator = segment.indexOf('=')
        if (separator > 0) {
            val key = segment.take(separator).trim().uppercase(Locale.ROOT)
            val raw = segment.substring(separator + 1).trim()
            if (key == "TYPE") {
                types += raw.split(',').map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotEmpty() }
            } else {
                params.putIfAbsent(key, raw)
            }
        } else {
            segment
                .trim()
                .uppercase(Locale.ROOT)
                .takeIf { it.isNotEmpty() }
                ?.let(types::add)
        }
    }
    val value = decodeIfQuotedPrintable(rawValue, params)
    return if (name.isEmpty()) {
        null
    } else {
        VCardProperty(name = name, params = params, types = types, value = value, group = group)
    }
}

/** Значение в quoted-printable: «=XX» — байт, кодировка — из параметра CHARSET. */
private fun decodeIfQuotedPrintable(
    value: String,
    params: Map<String, String>,
): String {
    val charset =
        params[CHARSET_KEY]?.takeIf { it.equals(QUOTED_PRINTABLE, ignoreCase = true) }
            ?: params[TYPE_KEY]?.takeIf { it.contains(QUOTED_PRINTABLE, ignoreCase = true) }
            ?: return value
    val encoding =
        runCatching { Charset.forName(charset.takeIf { !it.equals(QUOTED_PRINTABLE, true) } ?: "UTF-8") }
            .getOrDefault(Charsets.UTF_8)
    return decodeQuotedPrintable(value, encoding)
}

/** Кодировка значения из параметра CHARSET. */
private const val CHARSET_KEY = "CHARSET"

/** Параметр TYPE со значением quoted-printable. */
private const val TYPE_KEY = "TYPE"

/** Признак quoted-printable. */
private const val QUOTED_PRINTABLE = "QUOTED-PRINTABLE"

// Подавление осознанное: накопитель держит по маленькому обработчику на тип свойства.
@Suppress("TooManyFunctions")
private class VCardAccumulator {
    private var displayName = ""
    private var parts: NameParts? = null
    private val phones = mutableListOf<EditablePhone>()
    private val emails = mutableListOf<EditableEmail>()
    private val addresses = mutableListOf<EditableAddress>()
    private val websites = mutableListOf<EditableWebsite>()
    private val instantMessages = mutableListOf<EditableInstantMessage>()
    private val sipAddresses = mutableListOf<EditableSipAddress>()
    private val relations = mutableListOf<EditableRelation>()
    private val events = mutableListOf<EditableEvent>()
    private var company = ""
    private var title = ""
    private var department = ""
    private var jobDescription = ""
    private var officeLocation = ""
    private var note = ""
    private var nickname = ""
    private var birthday: Birthday? = null
    private var photo: ByteArray? = null
    private var photoSkipped = false
    private val categories = mutableListOf<String>()

    /** Было ли в блоке хотя бы одно поддерживаемое свойство. */
    private var sawProperty = false

    /** Подписи itemN.X-ABLabel применяются к последнему элементу группы itemN. */
    private val groupLabelAppliers = mutableMapOf<String, (String) -> Unit>()

    fun apply(line: String) {
        val property = parseVCardProperty(line) ?: return
        if (property.name == "X-ABLABEL") {
            applyGroupLabel(property)
            return
        }
        val value = unescapeVCard(property.value)
        if (property.name in SUPPORTED_PROPERTIES) sawProperty = true
        when (property.name) {
            "FN", "N", "ORG", "TITLE", "NOTE", "BDAY", "NICKNAME" -> applyIdentityProperty(property, value)
            "ROLE", "X-OFFICE-LOCATION" -> applyIdentityProperty(property, value)
            "TEL", "EMAIL", "ADR", "URL", "IMPP" -> applyCommunicationProperty(property, value)
            "ANNIVERSARY", "X-ABDATE", "RELATED", "X-ABRELATEDNAMES" -> applyExtraProperty(property, value)
            "PHOTO" -> applyPhoto(property)
            "CATEGORIES" -> applyCategories(property.value)
        }
    }

    /** Фото: base64 в свойстве PHOTO. Слишком большое и неподдерживаемое — контакт без фото. */
    private fun applyPhoto(property: VCardProperty) {
        if (property.params["VALUE"].equals("uri", ignoreCase = true)) return
        val payload = decodePhoto(property.value)
        when (payload.status) {
            PhotoStatus.OK -> photo = payload.bytes
            PhotoStatus.TOO_LARGE -> photoSkipped = true
            PhotoStatus.NONE,
            PhotoStatus.UNSUPPORTED,
            -> Unit
        }
    }

    /** Категории (названия групп): список через запятую. Значение приходит сырым, экранирование снимается по частям. */
    private fun applyCategories(raw: String) {
        splitVCardList(raw)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { category ->
                if (category !in categories) categories += category
            }
    }

    private fun applyGroupLabel(property: VCardProperty) {
        val group = property.group ?: return
        val label = cleanVCardLabel(unescapeVCard(property.value))
        if (label.isNotEmpty()) {
            groupLabelAppliers[group]?.invoke(label)
        }
    }

    /**
     * Запоминает, как применить подпись группы: строка itemN.X-ABLabel идёт
     * после самого свойства и уточняет уже добавленный элемент.
     */
    private fun <T> rememberGroupLabel(
        property: VCardProperty,
        items: MutableList<T>,
        index: Int,
        withLabel: (T, String) -> T,
    ) {
        val group = property.group ?: return
        groupLabelAppliers[group] = { label -> items[index] = withLabel(items[index], label) }
    }

    private fun phoneWithLabel(
        phone: EditablePhone,
        label: String,
    ): EditablePhone =
        phoneKindOfVCardLabel(label)?.let { phone.copy(kind = it) }
            ?: phone.copy(kind = PhoneKind.OTHER, customLabel = label)

    private fun emailWithLabel(
        email: EditableEmail,
        label: String,
    ): EditableEmail =
        emailKindOfVCardLabel(label)?.let { email.copy(kind = it) }
            ?: email.copy(kind = EmailKind.OTHER, customLabel = label)

    private fun addressWithLabel(
        address: EditableAddress,
        label: String,
    ): EditableAddress =
        addressKindOfVCardLabel(label)?.let { address.copy(kind = it) }
            ?: address.copy(kind = AddressKind.OTHER, customLabel = label)

    private fun applyIdentityProperty(
        property: VCardProperty,
        value: String,
    ) {
        when (property.name) {
            "FN" -> displayName = value
            "N" -> parts = namePartsFromVCard(property.value)
            "ORG" -> {
                val orgParts = splitVCardParts(property.value)
                company = orgParts.firstOrNull().orEmpty().trim()
                department = orgParts.getOrNull(1).orEmpty().trim()
            }
            "TITLE" -> title = value.trim()
            "ROLE" -> jobDescription = value.trim()
            "X-OFFICE-LOCATION" -> officeLocation = value.trim()
            "NOTE" -> note = value
            "BDAY" -> birthday = birthdayFromVCard(value)
            "NICKNAME" -> nickname = value.trim()
        }
    }

    private fun applyCommunicationProperty(
        property: VCardProperty,
        value: String,
    ) {
        when (property.name) {
            "TEL" -> addPhoneProperty(property, value)
            "EMAIL" -> addEmailProperty(property, value)
            "ADR" -> addAddressProperty(property)
            "URL" -> addWebsiteProperty(property, value)
            "IMPP" -> addImppProperty(property, value)
        }
    }

    private fun addPhoneProperty(
        property: VCardProperty,
        value: String,
    ) {
        val number = cleanPhoneNumber(value)
        if (number.isBlank()) return
        phones +=
            EditablePhone(
                number = number,
                kind = phoneKindOfVCard(property.types),
                isPrimary = "PREF" in property.types,
            )
        rememberGroupLabel(property, phones, phones.lastIndex, ::phoneWithLabel)
    }

    private fun addEmailProperty(
        property: VCardProperty,
        value: String,
    ) {
        if (value.isBlank()) return
        emails +=
            EditableEmail(
                address = value,
                kind = emailKindOfVCard(property.types),
                isPrimary = "PREF" in property.types,
            )
        rememberGroupLabel(property, emails, emails.lastIndex, ::emailWithLabel)
    }

    private fun addWebsiteProperty(
        property: VCardProperty,
        value: String,
    ) {
        if (value.isBlank()) return
        websites +=
            EditableWebsite(
                url = value,
                kind = websiteKindOfVCard(property.types),
                customLabel = vcardLabelOf(property.params),
            )
        rememberGroupLabel(property, websites, websites.lastIndex, ::websiteWithLabel)
    }

    private fun addImppProperty(
        property: VCardProperty,
        value: String,
    ) {
        if (value.isBlank()) return
        val isSip =
            property.params["X-SERVICE-TYPE"].equals("SIP", ignoreCase = true) ||
                isSipAddress(value)
        if (isSip) {
            sipAddresses +=
                EditableSipAddress(
                    address = value,
                    kind = sipKindOfVCard(property.types),
                    customLabel = vcardLabelOf(property.params),
                )
            rememberGroupLabel(property, sipAddresses, sipAddresses.lastIndex, ::sipWithLabel)
        } else {
            instantMessages +=
                instantMessageFromVCard(
                    service = property.params["X-SERVICE-TYPE"],
                    address = value,
                )
        }
    }

    /** Адрес: ADR-части сохраняются структурированно и дублируются в legacy-строку. */
    private fun addAddressProperty(property: VCardProperty) {
        val parts = splitVCardParts(property.value) + List(ADDRESS_PART_COUNT) { "" }
        val joined = parts.filter { it.isNotBlank() }.joinToString(", ")
        if (joined.isNotBlank()) {
            addresses +=
                EditableAddress(
                    address = joined,
                    kind = addressKindOfVCard(property.types),
                    poBox = parts[0].trim(),
                    neighborhood = parts[1].trim(),
                    street = parts[2].trim(),
                    city = parts[3].trim(),
                    region = parts[4].trim(),
                    postcode = parts[5].trim(),
                    country = parts[6].trim(),
                )
            rememberGroupLabel(property, addresses, addresses.lastIndex, ::addressWithLabel)
        }
    }

    private fun websiteWithLabel(
        website: EditableWebsite,
        label: String,
    ): EditableWebsite =
        websiteKindOfVCardLabel(label)?.let { website.copy(kind = it, customLabel = null) }
            ?: website.copy(kind = WebsiteKind.OTHER, customLabel = label)

    private fun sipWithLabel(
        sip: EditableSipAddress,
        label: String,
    ): EditableSipAddress =
        sipKindOfVCardLabel(label)?.let { sip.copy(kind = it, customLabel = null) }
            ?: sip.copy(kind = SipKind.OTHER, customLabel = label)

    private fun isSipAddress(value: String): Boolean =
        value.substringBefore(':', missingDelimiterValue = "").lowercase(Locale.ROOT) in SIP_SCHEMES

    private fun applyExtraProperty(
        property: VCardProperty,
        value: String,
    ) {
        when (property.name) {
            "ANNIVERSARY" ->
                birthdayFromVCard(value)?.let { date ->
                    events += EditableEvent(date = date, kind = EventKind.ANNIVERSARY)
                }
            "X-ABDATE" ->
                birthdayFromVCard(value)?.let { date ->
                    events +=
                        EditableEvent(
                            date = date,
                            kind = EventKind.OTHER,
                            customLabel = vcardLabelOf(property.params),
                        )
                    rememberGroupLabel(property, events, events.lastIndex) { event, label ->
                        eventKindOfVCardLabel(label)?.let { event.copy(kind = it, customLabel = null) }
                            ?: event.copy(kind = EventKind.OTHER, customLabel = label)
                    }
                }
            "RELATED", "X-ABRELATEDNAMES" ->
                if (value.isNotBlank()) {
                    val kind = relationKindOfVCard(property.types)
                    relations +=
                        EditableRelation(
                            name = value,
                            kind = kind,
                            customLabel = vcardLabelOf(property.params).takeIf { kind == RelationKind.OTHER },
                        )
                    rememberGroupLabel(property, relations, relations.lastIndex) { relation, label ->
                        relation.copy(kind = RelationKind.OTHER, customLabel = label)
                    }
                }
        }
    }

    fun build(): VCardBlock {
        val form =
            ContactForm(
                name = buildName(),
                phones = phones,
                emails = emails,
                addresses = addresses,
                websites = websites,
                instantMessages = instantMessages,
                sipAddresses = sipAddresses,
                relations = relations,
                events = events,
                organization = buildOrganization(),
                note = EditableNote(note = note),
                nickname = EditableNickname(nickname = nickname),
                birthday = EditableBirthday(value = birthday),
            )
        if (isEmptyForImport(form)) {
            val issue = if (sawProperty) VCardIssue.EMPTY else VCardIssue.UNSUPPORTED
            return VCardBlock.Skipped(displayName, issue)
        }
        return VCardBlock.Parsed(
            VCardContact(
                form = form,
                photo = photo,
                categories = categories.toList(),
                photoSkipped = photoSkipped,
            ),
        )
    }

    private fun buildName(): EditableName {
        val nameParts = parts
        return when {
            nameParts != null && !nameParts.isEmpty ->
                EditableName(
                    mode = NameMode.STRUCTURED,
                    given = nameParts.given,
                    family = nameParts.family,
                    middle = nameParts.middle,
                    prefix = nameParts.prefix,
                    suffix = nameParts.suffix,
                )
            displayName.isNotBlank() -> EditableName(display = displayName)
            else -> EditableName()
        }
    }

    private fun buildOrganization(): EditableOrganization =
        EditableOrganization(
            company = company,
            title = title,
            department = department,
            jobDescription = jobDescription,
            officeLocation = officeLocation,
        )
}

private fun cleanPhoneNumber(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.startsWith(TEL_URI_PREFIX, ignoreCase = true)) {
        trimmed.substring(TEL_URI_PREFIX.length).trim()
    } else {
        trimmed
    }
}

private fun isEmptyForImport(form: ContactForm): Boolean {
    val lists =
        listOf(
            form.phones,
            form.emails,
            form.addresses,
            form.websites,
            form.instantMessages,
            form.sipAddresses,
            form.relations,
            form.events,
        )
    return form.name.isEmpty &&
        lists.all { it.isEmpty() } &&
        form.organization.isEmpty &&
        form.note.note.isBlank() &&
        form.nickname.nickname.isBlank() &&
        form.birthday.value == null
}

private const val ADDRESS_PART_COUNT = 7

private val SIP_SCHEMES = setOf("sip", "sips")
