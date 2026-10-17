package com.m4isper.contactsapp.data.datasource

import android.content.ContentProviderOperation
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableName

private fun dataInsert(
    mimetype: String,
    rawContactId: Long? = null,
    rawContactBackRef: Int? = null,
): ContentProviderOperation.Builder {
    val builder =
        ContentProviderOperation
            .newInsert(ContactsContract.Data.CONTENT_URI)
            .withValue(ContactsContract.Data.MIMETYPE, mimetype)
    if (rawContactBackRef != null) {
        builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactBackRef)
    } else {
        builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
    }
    return builder
}

private fun dataUpdate(dataId: Long): ContentProviderOperation.Builder =
    ContentProviderOperation
        .newUpdate(ContactsContract.Data.CONTENT_URI)
        .withSelection(
            "${ContactsContract.Data._ID}=?",
            arrayOf(dataId.toString()),
        )

private fun deleteDataOperation(dataId: Long): ContentProviderOperation =
    ContentProviderOperation
        .newDelete(ContactsContract.Data.CONTENT_URI)
        .withSelection(
            "${ContactsContract.Data._ID}=?",
            arrayOf(dataId.toString()),
        ).build()

/** Операции создания контакта в локальном аккаунте. */
internal fun buildCreateOperations(
    form: ContactForm,
    photo: ByteArray?,
): ArrayList<ContentProviderOperation> {
    val ops = ArrayList<ContentProviderOperation>()
    ops +=
        ContentProviderOperation
            .newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
    if (!form.name.isEmpty) {
        ops +=
            dataInsert(StructuredName.CONTENT_ITEM_TYPE, rawContactBackRef = 0)
                .withValues(structuredNameValues(form.name))
                .build()
    }
    val values =
        buildList {
            form.phones
                .filter { it.number.isNotBlank() }
                .forEach { add(Phone.CONTENT_ITEM_TYPE to phoneValues(it)) }
            form.emails
                .filter { it.address.isNotBlank() }
                .forEach { add(Email.CONTENT_ITEM_TYPE to emailValues(it)) }
            form.addresses
                .filter { it.address.isNotBlank() }
                .forEach { add(StructuredPostal.CONTENT_ITEM_TYPE to addressValues(it)) }
            form.websites
                .filter { it.url.isNotBlank() }
                .forEach { add(Website.CONTENT_ITEM_TYPE to websiteValues(it)) }
            if (!form.organization.isEmpty) {
                add(Organization.CONTENT_ITEM_TYPE to organizationValues(form.organization))
            }
            if (form.note.note.isNotBlank()) {
                add(Note.CONTENT_ITEM_TYPE to noteValues(form.note.note))
            }
            form.birthday.value?.let { add(Event.CONTENT_ITEM_TYPE to eventValues(it)) }
            if (photo != null) add(Photo.CONTENT_ITEM_TYPE to photoValues(photo))
        }
    values.forEach { (mimetype, contentValues) ->
        ops += dataInsert(mimetype, rawContactBackRef = 0).withValues(contentValues).build()
    }
    return ops
}

/**
 * Операции обновления: имя и одиночные поля (организация, заметка, ДР) —
 * update/insert/delete по наличию строки, списки — diff по dataId.
 */
internal fun buildUpdateOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
    photo: ByteArray?,
): ArrayList<ContentProviderOperation> {
    val ops = ArrayList<ContentProviderOperation>()
    ops += nameOperations(rawContactId, rows.names, form.name)
    ops += listOperations(rawContactId, rows, form)
    ops += singleOperations(rawContactId, rows, form)
    ops += photoOperations(rawContactId, form.photoChanged, photo)
    return ops
}

/**
 * Операции имени по решению [decideNameActions]: строки всех raw contacts
 * обновляются или удаляются, новая вставляется только в целевой raw.
 */
private fun nameOperations(
    rawContactId: Long,
    existing: List<ExistingName>,
    name: EditableName,
): List<ContentProviderOperation> {
    val actions = decideNameActions(existing.map { it.dataId }, name.isEmpty)
    val ops = mutableListOf<ContentProviderOperation>()
    actions.deleteIds.forEach { ops += deleteDataOperation(it) }
    actions.updateIds.forEach { dataId ->
        ops += dataUpdate(dataId).withValues(structuredNameValues(name)).build()
    }
    if (actions.insertNew) {
        ops +=
            dataInsert(StructuredName.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(structuredNameValues(name))
                .build()
    }
    return ops
}

private fun listOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> {
    val ops = mutableListOf<ContentProviderOperation>()
    ops +=
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
    ops +=
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
    ops +=
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
    ops +=
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
    return ops
}

private fun singleOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> {
    val ops = mutableListOf<ContentProviderOperation>()
    ops +=
        singleValueOperations(
            existingDataId = rows.organization?.dataId,
            isEmpty = form.organization.isEmpty,
            insert = {
                dataInsert(Organization.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                    .withValues(organizationValues(form.organization))
            },
            update = { dataId ->
                dataUpdate(dataId).withValues(organizationValues(form.organization))
            },
        )
    val noteText = form.note.note
    ops +=
        singleValueOperations(
            existingDataId = rows.note?.dataId,
            isEmpty = noteText.isBlank(),
            insert = {
                dataInsert(Note.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                    .withValues(noteValues(noteText))
            },
            update = { dataId -> dataUpdate(dataId).withValues(noteValues(noteText)) },
        )
    val birthday = form.birthday.value
    ops +=
        when {
            birthday == null ->
                rows.birthday?.let { listOf(deleteDataOperation(it.dataId)) }.orEmpty()
            rows.birthday != null ->
                if (rows.birthday.raw == birthday.toStorageString()) {
                    emptyList()
                } else {
                    listOf(dataUpdate(rows.birthday.dataId).withValues(eventValues(birthday)).build())
                }
            else ->
                listOf(
                    dataInsert(Event.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                        .withValues(eventValues(birthday))
                        .build(),
                )
        }
    return ops
}

private fun photoOperations(
    rawContactId: Long,
    photoChanged: Boolean,
    photo: ByteArray?,
): List<ContentProviderOperation> {
    if (!photoChanged) return emptyList()
    val ops = mutableListOf<ContentProviderOperation>()
    ops +=
        ContentProviderOperation
            .newDelete(ContactsContract.Data.CONTENT_URI)
            .withSelection(
                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(rawContactId.toString(), Photo.CONTENT_ITEM_TYPE),
            ).build()
    if (photo != null) {
        ops +=
            dataInsert(Photo.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(photoValues(photo))
                .build()
    }
    return ops
}

/**
 * Операции списка: удаляем пропавшие строки, обновляем совпавшие по dataId,
 * вставляем новые.
 */
private fun <T> diffListOperations(
    newItems: List<T>,
    existingIds: Set<Long>,
    dataIdOf: (T) -> Long?,
    insert: (T) -> ContentProviderOperation.Builder,
    update: (Long, T) -> ContentProviderOperation.Builder,
): List<ContentProviderOperation> {
    val ops = mutableListOf<ContentProviderOperation>()
    val newIds = newItems.mapNotNullTo(mutableSetOf(), dataIdOf)
    existingIds.filter { it !in newIds }.forEach { removed -> ops += deleteDataOperation(removed) }
    newItems.forEach { item ->
        val existingId = dataIdOf(item)?.takeIf { it in existingIds }
        ops +=
            if (existingId == null) {
                insert(item).build()
            } else {
                update(existingId, item).build()
            }
    }
    return ops
}

private fun singleValueOperations(
    existingDataId: Long?,
    isEmpty: Boolean,
    insert: () -> ContentProviderOperation.Builder,
    update: (Long) -> ContentProviderOperation.Builder,
): List<ContentProviderOperation> =
    when {
        isEmpty -> existingDataId?.let { listOf(deleteDataOperation(it)) }.orEmpty()
        existingDataId == null -> listOf(insert().build())
        else -> listOf(update(existingDataId).build())
    }
