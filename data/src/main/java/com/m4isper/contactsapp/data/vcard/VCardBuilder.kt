package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.data.format.birthdayToString
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
import com.m4isper.contactsapp.domain.model.SipAddress
import com.m4isper.contactsapp.domain.model.SipKind
import com.m4isper.contactsapp.domain.model.Website
import com.m4isper.contactsapp.domain.model.WebsiteKind

/** Сборка vCard 3.0 из контактов: обратная сторона [parseVCards]. */

internal fun vcardType(kind: PhoneKind): String =
    when (kind) {
        PhoneKind.MOBILE -> "CELL"
        PhoneKind.HOME -> "HOME"
        PhoneKind.WORK -> "WORK"
        PhoneKind.WORK_MOBILE -> "WORK,CELL"
        PhoneKind.FAX -> "FAX"
        PhoneKind.PAGER -> "PAGER"
        PhoneKind.CALLBACK -> "CALLBACK"
        PhoneKind.COMPANY_MAIN -> "COMPANY_MAIN"
        PhoneKind.OTHER -> "VOICE"
    }

internal fun vcardEmailType(kind: EmailKind): String =
    when (kind) {
        EmailKind.HOME -> "HOME"
        EmailKind.WORK -> "WORK"
        EmailKind.MOBILE -> "CELL"
        EmailKind.OTHER -> "OTHER"
    }

internal fun vcardAddressType(kind: AddressKind): String =
    when (kind) {
        AddressKind.HOME -> "HOME"
        AddressKind.WORK -> "WORK"
        AddressKind.OTHER -> "OTHER"
    }

internal fun vcardWebsiteType(kind: WebsiteKind): String? =
    when (kind) {
        WebsiteKind.HOMEPAGE -> null
        WebsiteKind.HOME -> "HOME"
        WebsiteKind.WORK -> "WORK"
        WebsiteKind.BLOG -> "BLOG"
        WebsiteKind.PROFILE -> "PROFILE"
        WebsiteKind.FTP -> "FTP"
        WebsiteKind.OTHER -> "OTHER"
    }

internal fun vcardSipType(kind: SipKind): String =
    when (kind) {
        SipKind.HOME -> "HOME"
        SipKind.WORK -> "WORK"
        SipKind.OTHER -> "OTHER"
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
        details.sipAddresses.forEach(::writeSip)
        details.addresses.forEach(::writeAddress)
        details.websites.forEach(::writeWebsite)
        details.organization?.let { organization ->
            if (organization.company.isNotBlank() || organization.department.isNotBlank()) {
                val value =
                    if (organization.department.isBlank()) {
                        escapeVCard(organization.company)
                    } else {
                        "${escapeVCard(organization.company)};${escapeVCard(organization.department)}"
                    }
                writeProperty("ORG", value)
            }
            if (organization.title.isNotBlank()) {
                writeProperty("TITLE", escapeVCard(organization.title))
            }
            if (organization.jobDescription.isNotBlank()) {
                writeProperty("ROLE", escapeVCard(organization.jobDescription))
            }
            if (organization.officeLocation.isNotBlank()) {
                writeProperty("X-OFFICE-LOCATION", escapeVCard(organization.officeLocation))
            }
        }
        details.note?.let { writeProperty("NOTE", escapeVCard(it)) }
        details.birthday?.let { writeProperty("BDAY", birthdayToString(it)) }
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
        val type = if (phone.isPrimary) "${vcardType(phone.kind)},PREF" else vcardType(phone.kind)
        writeProperty(
            "TEL;TYPE=$type",
            escapeVCard(phone.number),
            phone.customLabel?.takeIf { phone.kind == PhoneKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeEmail(email: EmailAddress) {
        val type = if (email.isPrimary) "${vcardEmailType(email.kind)},PREF" else vcardEmailType(email.kind)
        writeProperty(
            "EMAIL;TYPE=$type",
            escapeVCard(email.address),
            email.customLabel?.takeIf { email.kind == EmailKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeSip(sip: SipAddress) {
        writeProperty(
            "IMPP;X-SERVICE-TYPE=SIP;TYPE=${vcardSipType(sip.kind)}",
            escapeVCard(sip.address),
            sip.customLabel?.takeIf { sip.kind == SipKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeWebsite(website: Website) {
        val type = vcardWebsiteType(website.kind)
        writeProperty(
            if (type == null) "URL" else "URL;TYPE=$type",
            escapeVCard(website.url),
            website.customLabel?.takeIf { website.kind == WebsiteKind.OTHER && it.isNotBlank() },
        )
    }

    private fun writeAddress(address: PostalAddress) {
        writeProperty(
            "ADR;TYPE=${vcardAddressType(address.kind)}",
            listOf(
                address.poBox,
                address.neighborhood,
                address.streetLine,
                address.city,
                address.region,
                address.postcode,
                address.country,
            ).joinToString(";") { escapeVCard(it) },
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
            birthdayToString(event.date),
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
