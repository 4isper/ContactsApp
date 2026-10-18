package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactEvent
import com.m4isper.contactsapp.domain.model.ContactRelation
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.InstantMessage
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.PostalAddress
import com.m4isper.contactsapp.domain.model.RelationKind

/** Сборка vCard 3.0 из контактов: обратная сторона [parseVCards]. */

internal fun vcardType(kind: PhoneKind): String =
    when (kind) {
        PhoneKind.MOBILE -> "CELL"
        PhoneKind.HOME -> "HOME"
        PhoneKind.WORK -> "WORK"
        PhoneKind.OTHER -> "VOICE"
    }

internal fun vcardEmailType(kind: EmailKind): String =
    when (kind) {
        EmailKind.HOME -> "HOME"
        EmailKind.WORK -> "WORK"
        EmailKind.OTHER -> "OTHER"
    }

internal fun vcardAddressType(kind: AddressKind): String =
    when (kind) {
        AddressKind.HOME -> "HOME"
        AddressKind.WORK -> "WORK"
        AddressKind.OTHER -> "OTHER"
    }

/** Имена сервисов, которые понимает vCard-импортёр Android (VCardUtils). */
internal fun vcardImService(im: InstantMessage): String =
    when (im.protocol) {
        ImProtocol.AIM -> "AIM"
        ImProtocol.MSN -> "MSN"
        ImProtocol.YAHOO -> "Yahoo"
        ImProtocol.SKYPE -> "Skype"
        ImProtocol.QQ -> "QQ"
        ImProtocol.GOOGLE_TALK -> "Google Talk"
        ImProtocol.ICQ -> "ICQ"
        ImProtocol.JABBER -> "Jabber"
        ImProtocol.CUSTOM -> im.customProtocol?.takeIf { it.isNotBlank() } ?: "Custom"
    }

/** Экранирование значения: переводы строк — \n, служебные символы — с обратным слешем. */
internal fun escapeVCard(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\r", "\\n")
        .replace("\n", "\\n")

/**
 * Пишет строки одного vCard: подписи, которые не выражаются TYPE,
 * выносятся в группу itemN (формат Apple/Android) — itemN.NAME + itemN.X-ABLabel.
 */
private class VCardWriter {
    private val lines = mutableListOf<String>()
    private var group = 0

    fun write(details: ContactDetails): String {
        lines += "BEGIN:VCARD"
        lines += "VERSION:3.0"
        writeName(details)
        details.phones.forEach(::writePhone)
        details.emails.forEach(::writeEmail)
        details.instantMessages.forEach { im ->
            writeProperty("IMPP;X-SERVICE-TYPE=${vcardImService(im)}", escapeVCard(im.address))
        }
        details.addresses.forEach(::writeAddress)
        details.websites.forEach { website ->
            writeProperty("URL", escapeVCard(website.url))
        }
        details.organization?.let { organization ->
            if (organization.company.isNotBlank()) {
                writeProperty("ORG", escapeVCard(organization.company))
            }
            if (organization.title.isNotBlank()) {
                writeProperty("TITLE", escapeVCard(organization.title))
            }
        }
        details.note?.let { writeProperty("NOTE", escapeVCard(it)) }
        details.birthday?.let { writeProperty("BDAY", it.toStorageString()) }
        details.nickname?.let { writeProperty("NICKNAME", escapeVCard(it)) }
        details.relations.forEach(::writeRelation)
        details.events.forEach(::writeEvent)
        lines += "END:VCARD"
        return lines.joinToString(separator = "\n", postfix = "\n")
    }

    private fun writeName(details: ContactDetails) {
        writeProperty("FN", escapeVCard(details.name))
        details.nameParts?.let { parts ->
            writeProperty(
                "N",
                "${escapeVCard(parts.family)};${escapeVCard(parts.given)};" +
                    "${escapeVCard(parts.middle)};${escapeVCard(parts.prefix)};${escapeVCard(parts.suffix)}",
            )
        }
    }

    private fun writePhone(phone: PhoneNumber) {
        writeProperty(
            "TEL;TYPE=${vcardType(phone.kind)}",
            escapeVCard(phone.number),
            phone.customLabel?.takeIf { phone.kind == PhoneKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeEmail(email: EmailAddress) {
        writeProperty(
            "EMAIL;TYPE=${vcardEmailType(email.kind)}",
            escapeVCard(email.address),
            email.customLabel?.takeIf { email.kind == EmailKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeAddress(address: PostalAddress) {
        writeProperty(
            "ADR;TYPE=${vcardAddressType(address.kind)}",
            ";;${escapeVCard(address.address)};;;;",
            address.customLabel?.takeIf { address.kind == AddressKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeRelation(relation: ContactRelation) {
        writeProperty(
            "RELATED;TYPE=${vcardRelationType(relation.kind)}",
            escapeVCard(relation.name),
            relation.customLabel?.takeIf { relation.kind == RelationKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeEvent(event: ContactEvent) {
        writeProperty(
            if (event.kind == EventKind.ANNIVERSARY) "ANNIVERSARY" else "X-ABDATE",
            event.date.toStorageString(),
            event.customLabel?.takeIf { event.kind == EventKind.OTHER && it.isNotBlank() },
        )
    }

    /** Строка свойства; с подписью — группа itemN. Значение приходит уже экранированным. */
    private fun writeProperty(
        head: String,
        escapedValue: String,
        label: String? = null,
    ) {
        if (label == null) {
            lines += "$head:$escapedValue"
        } else {
            group += 1
            lines += "item$group.$head:$escapedValue"
            lines += "item$group.X-ABLabel:${escapeVCard(label)}"
        }
    }
}

internal fun buildVCard(details: ContactDetails): String = VCardWriter().write(details)

/** Склеенный поток из нескольких vCard: валидный формат для файла .vcf с несколькими контактами. */
internal fun buildVCards(contacts: List<ContactDetails>): String =
    contacts.joinToString(separator = "") { buildVCard(it) }
