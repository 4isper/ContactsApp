package com.m4isper.contactsapp.data.datasource

import android.content.ContentProviderOperation
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.domain.model.ContactForm

/** Телефоны: diff формы и существующих строк (вставка, обновление, удаление). */
internal fun phoneListOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> =
    diffListOperations(
        newItems = form.phones.filter { it.number.isNotBlank() },
        existingIds = rows.phones.mapTo(mutableSetOf()) { it.dataId },
        dataIdOf = { it.dataId },
        insert = { phone ->
            dataInsert(Phone.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(phoneValues(phone))
        },
        update = { dataId, phone -> dataUpdate(dataId).withValues(phoneValues(phone)) },
    )

/** Почта: diff формы и существующих строк (вставка, обновление, удаление). */
internal fun emailListOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> =
    diffListOperations(
        newItems = form.emails.filter { it.address.isNotBlank() },
        existingIds = rows.emails.mapTo(mutableSetOf()) { it.dataId },
        dataIdOf = { it.dataId },
        insert = { email ->
            dataInsert(Email.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(emailValues(email))
        },
        update = { dataId, email -> dataUpdate(dataId).withValues(emailValues(email)) },
    )

/** Адреса: diff формы и существующих строк (вставка, обновление, удаление). */
internal fun addressListOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> =
    diffListOperations(
        newItems = form.addresses.filter { it.address.isNotBlank() },
        existingIds = rows.addresses.mapTo(mutableSetOf()) { it.dataId },
        dataIdOf = { it.dataId },
        insert = { address ->
            dataInsert(StructuredPostal.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(addressValues(address))
        },
        update = { dataId, address -> dataUpdate(dataId).withValues(addressValues(address)) },
    )

/** Сайты: diff формы и существующих строк (вставка, обновление, удаление). */
internal fun websiteListOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> =
    diffListOperations(
        newItems = form.websites.filter { it.url.isNotBlank() },
        existingIds = rows.websites.mapTo(mutableSetOf()) { it.dataId },
        dataIdOf = { it.dataId },
        insert = { website ->
            dataInsert(Website.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(websiteValues(website))
        },
        update = { dataId, website -> dataUpdate(dataId).withValues(websiteValues(website)) },
    )
