package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.ContactDetails

/** Полная модель контакта для экрана деталей. */
internal fun buildContactDetails(
    header: ContactHeaderRow,
    rows: List<DataRow>,
): ContactDetails =
    ContactDetails(
        contactId = header.contactId,
        name = header.name,
        phones = phoneNumbersOf(rows),
        emails = emailAddressesOf(rows),
        addresses = postalAddressesOf(rows),
        websites = websitesOf(rows),
        organization = organizationOf(rows),
        note = noteOf(rows),
        birthday = birthdayOf(rows),
        photoUri = header.photoUri,
        nameParts = namePartsOf(rows),
    )

/**
 * Лёгкая агрегация для списка: контакты без телефона и email тоже попадают
 * в список (имена и фото берутся из таблицы Contacts).
 */
internal fun aggregateContacts(
    headers: List<ContactHeaderRow>,
    rows: List<DataRow>,
): List<ContactDetails> {
    val rowsByContact = rows.groupBy { it.contactId }
    return headers.mapNotNull { header ->
        if (header.name.isBlank()) return@mapNotNull null
        val contactRows = rowsByContact[header.contactId].orEmpty()
        ContactDetails(
            contactId = header.contactId,
            name = header.name,
            phones = phoneNumbersOf(contactRows),
            emails = emailAddressesOf(contactRows),
            photoUri = header.photoUri,
            nameParts = namePartsOf(contactRows),
        )
    }
}
