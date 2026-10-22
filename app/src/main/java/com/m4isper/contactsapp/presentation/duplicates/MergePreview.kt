package com.m4isper.contactsapp.presentation.duplicates

import com.m4isper.contactsapp.domain.model.ContactDetails

/** Поле, которого нет в основном контакте, но появится после слияния. */
data class MergeAddition(
    val field: MergeField,
    val value: String,
)

/** Тип поля контакта в предпросмотре слияния. */
enum class MergeField {
    NAME,
    PHONE,
    EMAIL,
    ADDRESS,
    WEBSITE,
    ORGANIZATION,
    NOTE,
    BIRTHDAY,
}

/**
 * Поля источников, которых нет в целевом контакте: после слияния они появятся
 * в общем контакте. Пустой список — сливать нечего, контакты уже совпадают.
 */
internal fun mergeAdditions(
    target: ContactDetails,
    sources: List<ContactDetails>,
): List<MergeAddition> {
    val targetPhones = target.phones.map { it.number }.toSet()
    val targetEmails = target.emails.map { it.address }.toSet()
    val targetAddresses = target.addresses.map { it.displayAddress }.toSet()
    val targetWebsites = target.websites.map { it.url }.toSet()
    val additions = mutableListOf<MergeAddition>()
    sources.forEach { source ->
        additions += nameAddition(target, source)
        additions += valuesAddition(MergeField.PHONE, targetPhones, source.phones.map { it.number })
        additions += valuesAddition(MergeField.EMAIL, targetEmails, source.emails.map { it.address })
        additions += valuesAddition(MergeField.ADDRESS, targetAddresses, source.addresses.map { it.displayAddress })
        additions += valuesAddition(MergeField.WEBSITE, targetWebsites, source.websites.map { it.url })
        additions += organizationAddition(target, source)
        additions += noteAddition(target, source)
        additions += birthdayAddition(target, source)
    }
    return additions.distinctBy { it.field to it.value }
}

/** Имя источника, если оно отличается от имени основного контакта. */
private fun nameAddition(
    target: ContactDetails,
    source: ContactDetails,
): List<MergeAddition> =
    if (source.name != target.name && source.name.isNotBlank()) {
        listOf(MergeAddition(MergeField.NAME, source.name))
    } else {
        emptyList()
    }

/** Значения поля, которых нет у основного контакта. */
private fun valuesAddition(
    field: MergeField,
    existing: Set<String>,
    values: List<String>,
): List<MergeAddition> = values.filter { it !in existing }.map { MergeAddition(field, it) }

/** Организация источника, если она отличается. */
private fun organizationAddition(
    target: ContactDetails,
    source: ContactDetails,
): List<MergeAddition> {
    val organization = source.organization
    return if (organization != null && organization != target.organization) {
        listOf(MergeAddition(MergeField.ORGANIZATION, organization.company))
    } else {
        emptyList()
    }
}

/** Заметка источника, если она отличается. */
private fun noteAddition(
    target: ContactDetails,
    source: ContactDetails,
): List<MergeAddition> {
    val note = source.note
    return if (!note.isNullOrBlank() && note != target.note) {
        listOf(MergeAddition(MergeField.NOTE, note))
    } else {
        emptyList()
    }
}

/** День рождения источника, если он отличается. */
private fun birthdayAddition(
    target: ContactDetails,
    source: ContactDetails,
): List<MergeAddition> =
    if (source.birthday != null && source.birthday != target.birthday) {
        listOf(MergeAddition(MergeField.BIRTHDAY, source.birthday.toString()))
    } else {
        emptyList()
    }
