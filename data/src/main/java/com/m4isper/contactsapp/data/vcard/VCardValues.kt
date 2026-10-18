package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.model.PhoneKind
import java.util.Locale

/** Разыменование vCard-экранирования: \n, \N и любой экранированный символ. */
internal fun unescapeVCard(raw: String): String {
    val result = StringBuilder(raw.length)
    var i = 0
    while (i < raw.length) {
        val char = raw[i]
        if (char == '\\' && i + 1 < raw.length) {
            val next = raw[i + 1]
            result.append(if (next == 'n' || next == 'N') '\n' else next)
            i += 2
        } else {
            result.append(char)
            i += 1
        }
    }
    return result.toString()
}

/** Разбиение структурированного значения (N, ADR, ORG) по незаэкранированным ';'. */
internal fun splitVCardParts(raw: String): List<String> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < raw.length) {
        val char = raw[i]
        when {
            char == '\\' && i + 1 < raw.length -> {
                current.append(char).append(raw[i + 1])
                i += 2
            }
            char == ';' -> {
                parts += unescapeVCard(current.toString())
                current.clear()
                i += 1
            }
            else -> {
                current.append(char)
                i += 1
            }
        }
    }
    parts += unescapeVCard(current.toString())
    return parts
}

/** N в vCard — это Family;Given;Middle;Prefix;Suffix. */
internal fun namePartsFromVCard(raw: String): NameParts {
    val parts = splitVCardParts(raw) + List(5) { "" }
    return NameParts(
        given = parts[1].trim(),
        family = parts[0].trim(),
        middle = parts[2].trim(),
        prefix = parts[3].trim(),
        suffix = parts[4].trim(),
    )
}

internal fun phoneKindOfVCard(types: Set<String>): PhoneKind =
    when {
        types.any { it == "CELL" || it == "MOBILE" } -> PhoneKind.MOBILE
        "HOME" in types -> PhoneKind.HOME
        "WORK" in types -> PhoneKind.WORK
        else -> PhoneKind.OTHER
    }

internal fun emailKindOfVCard(types: Set<String>): EmailKind =
    when {
        "WORK" in types -> EmailKind.WORK
        "HOME" in types -> EmailKind.HOME
        else -> EmailKind.OTHER
    }

internal fun addressKindOfVCard(types: Set<String>): AddressKind =
    when {
        "WORK" in types -> AddressKind.WORK
        "HOME" in types -> AddressKind.HOME
        else -> AddressKind.OTHER
    }

/** IMPP: протокол из X-SERVICE-TYPE или из схемы значения (skype:, xmpp: и т.п.). */
internal fun instantMessageFromVCard(
    service: String?,
    address: String,
): EditableInstantMessage {
    val byService = service?.let(::imProtocolOfVCard)
    if (byService != null) {
        return EditableInstantMessage(address = address, protocol = byService)
    }
    val scheme = address.substringBefore(':', missingDelimiterValue = "").lowercase(Locale.ROOT)
    val byScheme = imProtocolOfVCard(scheme)
    return when {
        byScheme != null -> EditableInstantMessage(address = address, protocol = byScheme)
        !service.isNullOrBlank() ->
            EditableInstantMessage(
                address = address,
                protocol = ImProtocol.CUSTOM,
                customProtocol = service,
            )
        scheme.isNotBlank() ->
            EditableInstantMessage(
                address = address,
                protocol = ImProtocol.CUSTOM,
                customProtocol = scheme,
            )
        else -> EditableInstantMessage(address = address, protocol = ImProtocol.CUSTOM)
    }
}

private fun imProtocolOfVCard(service: String): ImProtocol? =
    when (service.trim().lowercase(Locale.ROOT)) {
        "aim" -> ImProtocol.AIM
        "msn" -> ImProtocol.MSN
        "yahoo" -> ImProtocol.YAHOO
        "skype" -> ImProtocol.SKYPE
        "qq" -> ImProtocol.QQ
        "google talk", "googletalk", "gtalk" -> ImProtocol.GOOGLE_TALK
        "icq" -> ImProtocol.ICQ
        "jabber", "xmpp" -> ImProtocol.JABBER
        else -> null
    }

private const val FULL_DATE_DIGITS = 8
private const val MONTH_DAY_DIGITS = 4
private const val MONTH_DIGITS = 2
private const val DAY_DIGITS = 2
private const val YEAR_DIGITS = 4
private const val LAST_MONTH = 12
private const val LAST_DAY = 31

/** BDAY: "yyyy-MM-dd", "yyyyMMdd", "--MM-dd", "--MMdd" (время после T отбрасывается). */
internal fun birthdayFromVCard(raw: String): Birthday? {
    val digits =
        raw
            .trim()
            .substringBefore('T')
            .replace("-", "")
    val body = digits.takeIf { it.length == FULL_DATE_DIGITS || it.length == MONTH_DAY_DIGITS }
    val monthDay = body?.takeLast(MONTH_DAY_DIGITS)
    val month = monthDay?.take(MONTH_DIGITS)?.toIntOrNull()
    val day = monthDay?.takeLast(DAY_DIGITS)?.toIntOrNull()
    val year = body?.takeIf { it.length == FULL_DATE_DIGITS }?.take(YEAR_DIGITS)?.toIntOrNull()
    return if (month == null || day == null) {
        null
    } else {
        Birthday(day = day, month = month, year = year)
            .takeIf { it.month in 1..LAST_MONTH && it.day in 1..LAST_DAY }
    }
}
