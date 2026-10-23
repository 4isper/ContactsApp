package com.m4isper.contactsapp.data.datasource.write.contacts

import com.m4isper.contactsapp.data.format.birthdayToString
import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.EditableSipAddress
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import java.util.Locale

private val WHITESPACE = Regex("\\s+")

/** Ключ телефона для сравнения дубликатов: только цифры. */
internal fun phoneKey(phone: EditablePhone): String? = phone.number.filter(Char::isDigit).takeIf(String::isNotEmpty)

/** Ключ почты для сравнения дубликатов: без регистра и лишних пробелов. */
internal fun emailKey(email: EditableEmail): String? = normalizedText(email.address)

internal fun addressKey(address: EditableAddress): String? =
    normalizedText(
        listOf(
            address.streetLine,
            address.poBox,
            address.neighborhood,
            address.city,
            address.region,
            address.postcode,
            address.country,
        ).joinToString("|"),
    )

internal fun websiteKey(website: EditableWebsite): String? = normalizedText(website.url)

internal fun imKey(im: EditableInstantMessage): String? = normalizedText(im.address)

internal fun sipKey(sip: EditableSipAddress): String? = normalizedText(sip.address)

internal fun relationKey(relation: EditableRelation): String? = normalizedText(relation.name)

/** Ключ события: вид и дата — разобранная из формы или неразобранная строка провайдера. */
internal fun eventKey(event: EditableEvent): String? {
    val raw = event.date?.let(::birthdayToString) ?: event.unparsedDate ?: return null
    return "${event.kind.name}|$raw"
}

private fun normalizedText(value: String): String? =
    value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(WHITESPACE, " ")
        .takeIf(String::isNotEmpty)
