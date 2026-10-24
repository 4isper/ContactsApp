package com.m4isper.contactsapp.data.datasource.read

import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.NameParts
import com.m4isper.contactsapp.domain.model.contact.PhotoRef

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
        sipAddresses = sipAddressesOf(rows),
        organization = organizationOf(rows),
        note = noteOf(rows),
        birthday = birthdayOf(rows),
        relations = relationsOf(rows),
        events = eventsOf(rows),
        nickname = nicknameOf(rows),
        photo = PhotoRef.of(header.photoUri),
        nameParts = namePartsOf(rows),
        account = account,
        lastUpdated = header.lastUpdated,
        lastContacted = header.lastContacted,
        timesContacted = header.timesContacted,
    )

/**
 * Лёгкая агрегация для списка: контакты без телефона и email тоже попадают
 * в список (имена и фото берутся из таблицы Contacts). Для поиска читаются
 * также организация, адреса, сайты, заметки, никнейм и связи.
 */
internal fun aggregateContacts(
    headers: List<ContactHeaderRow>,
    rows: List<DataRow>,
    accounts: Map<Long, ContactAccount> = emptyMap(),
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
            addresses = postalAddressesOf(contactRows),
            websites = websitesOf(contactRows),
            instantMessages = instantMessagesOf(contactRows),
            sipAddresses = sipAddressesOf(contactRows),
            organization = organizationOf(contactRows),
            note = noteOf(contactRows),
            birthday = birthdayOf(contactRows),
            relations = relationsOf(contactRows),
            events = eventsOf(contactRows),
            photo = PhotoRef.of(header.photoUri),
            nameParts = namePartsOf(contactRows),
            nickname = nicknameOf(contactRows),
            account = accounts[header.contactId] ?: ContactAccount.Local,
            lastUpdated = header.lastUpdated,
            lastContacted = header.lastContacted,
            timesContacted = header.timesContacted,
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
