package com.m4isper.contactsapp.data.datasource.read

import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.NameParts

/** Полная модель контакта для экрана деталей. */
internal fun buildContactDetails(
    header: ContactHeaderRow,
    rows: List<DataRow>,
    account: ContactAccount = ContactAccount.Local,
): ContactDetails =
    ContactDetails(
        contactId = header.contactId,
        name = header.name,
        phones = phoneNumbersOf(rows),
        emails = emailAddressesOf(rows),
        addresses = postalAddressesOf(rows),
        websites = websitesOf(rows),
        instantMessages = instantMessagesOf(rows),
        organization = organizationOf(rows),
        note = noteOf(rows),
        birthday = birthdayOf(rows),
        relations = relationsOf(rows),
        events = eventsOf(rows),
        nickname = nicknameOf(rows),
        photoUri = header.photoUri,
        nameParts = namePartsOf(rows),
        account = account,
    )

/**
 * Лёгкая агрегация для списка: контакты без телефона и email тоже попадают
 * в список (имена и фото берутся из таблицы Contacts). Никнейм и связи
 * нужны поиску, остальные поля деталей не читаются.
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
            relations = relationsOf(contactRows),
            nickname = nicknameOf(contactRows),
        )
    }
}

// Раскладка колонок StructuredName: data1 — display name, data2 — given, data3 — family,
// data4 — prefix, data5 — middle, data6 — suffix, data7 — phonetic given,
// data8 — phonetic middle, data9 — phonetic family.

/** Компоненты структурированного имени; null, если все они пустые. */
internal fun namePartsOf(rows: List<DataRow>): NameParts? =
    rows
        .firstOrNull { it.mimetype == StructuredName.CONTENT_ITEM_TYPE }
        ?.let { row ->
            NameParts(
                given = row.data2.orEmpty().trim(),
                family = row.data3.orEmpty().trim(),
                prefix = row.data4.orEmpty().trim(),
                middle = row.data5.orEmpty().trim(),
                suffix = row.data6.orEmpty().trim(),
                phoneticGiven = row.data7.orEmpty().trim(),
                phoneticMiddle = row.data8.orEmpty().trim(),
                phoneticFamily = row.data9.orEmpty().trim(),
            )
        }?.takeIf { !it.isEmpty }
