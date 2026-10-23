// Подавление осознанное: план записи разбит по типам строк контакта.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.data.datasource.write.contacts

import android.content.ContentProviderOperation
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.data.datasource.read.ExistingName
import com.m4isper.contactsapp.data.datasource.write.groups.membershipOperations
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableName

internal fun dataInsert(
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

internal fun dataUpdate(dataId: Long): ContentProviderOperation.Builder =
    ContentProviderOperation
        .newUpdate(ContactsContract.Data.CONTENT_URI)
        .withSelection(
            "${ContactsContract.Data._ID}=?",
            arrayOf(dataId.toString()),
        )

internal fun deleteDataOperation(dataId: Long): ContentProviderOperation =
    ContentProviderOperation
        .newDelete(ContactsContract.Data.CONTENT_URI)
        .withSelection(
            "${ContactsContract.Data._ID}=?",
            arrayOf(dataId.toString()),
        ).build()

/** Операции создания контакта в выбранном аккаунте; локальный — null account. */
internal fun buildCreateOperations(
    form: ContactForm,
    photo: ByteArray?,
    account: ContactAccount,
): ArrayList<ContentProviderOperation> {
    val ops = ArrayList<ContentProviderOperation>()
    ops +=
        ContentProviderOperation
            .newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
            .build()
    if (!form.name.isEmpty) {
        ops +=
            dataInsert(StructuredName.CONTENT_ITEM_TYPE, rawContactBackRef = 0)
                .withValues(structuredNameValues(form.name))
                .build()
    }
    contactDataValues(form, photo).forEach { (mimetype, contentValues) ->
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
    ops += imOperations(rawContactId, rows, form)
    ops += relationOperations(rawContactId, rows, form)
    ops += eventOperations(rawContactId, rows, form)
    ops += nicknameOperations(rawContactId, rows.nickname, form.nickname.nickname)
    ops += membershipOperations(rawContactId, rows.groupMemberships, form.groups.toSet())
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
): List<ContentProviderOperation> =
    phoneListOperations(rawContactId, rows, form) +
        emailListOperations(rawContactId, rows, form) +
        sipListOperations(rawContactId, rows, form) +
        addressListOperations(rawContactId, rows, form) +
        websiteListOperations(rawContactId, rows, form)

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
    ops += birthdayWriteOperations(rawContactId, rows.birthday, birthday)
    return ops
}

private fun photoOperations(
    rawContactId: Long,
    photoChanged: Boolean,
    photo: ByteArray?,
): List<ContentProviderOperation> {
    if (!photoChanged) return emptyList()
    return photoWriteOperations(rawContactId, photo)
}

/** Замена фото контакта: удаляем прежнее и записываем новое. */
internal fun photoWriteOperations(
    rawContactId: Long,
    photo: ByteArray?,
): List<ContentProviderOperation> {
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
internal fun <T> diffListOperations(
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
