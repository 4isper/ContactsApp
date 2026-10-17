package com.m4isper.contactsapp.data.datasource

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Relation
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.RelationKind

/**
 * Произвольная связь всегда пишется типом CUSTOM: у Relation нет TYPE_OTHER,
 * подпись при этом необязательна.
 */
internal fun relationValues(relation: EditableRelation): ContentValues =
    ContentValues().apply {
        val customLabel =
            relation.customLabel?.takeIf { relation.kind == RelationKind.OTHER && it.isNotBlank() }
        val type =
            if (relation.kind == RelationKind.OTHER) {
                Relation.TYPE_CUSTOM
            } else {
                relationTypeInt(relation.kind)
            }
        put(Relation.NAME, relation.name)
        put(Relation.TYPE, type)
        put(Relation.LABEL, customLabel)
    }

internal fun relationOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> =
    diffListOperations(
        newItems = form.relations.filter { it.name.isNotBlank() },
        existingIds = rows.relations.mapTo(mutableSetOf()) { it.dataId },
        dataIdOf = { it.dataId },
        insert = { relation ->
            dataInsert(Relation.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(relationValues(relation))
        },
        update = { dataId, relation -> dataUpdate(dataId).withValues(relationValues(relation)) },
    )

internal fun eventTypeInt(event: EditableEvent): Int =
    when {
        event.kind == EventKind.ANNIVERSARY -> Event.TYPE_ANNIVERSARY
        !event.customLabel.isNullOrBlank() -> Event.TYPE_CUSTOM
        else -> Event.TYPE_OTHER
    }

internal fun contactEventValues(
    event: EditableEvent,
    date: Birthday,
): ContentValues =
    ContentValues().apply {
        put(Event.START_DATE, date.toStorageString())
        put(Event.TYPE, eventTypeInt(event))
        put(Event.LABEL, event.customLabel.takeIf { event.kind == EventKind.OTHER && !it.isNullOrBlank() })
    }

/** События без даты не сохраняются: строка события без даты бессмысленна. */
internal fun eventOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> {
    val dated =
        form.events.mapNotNull { event -> event.date?.let { date -> event to date } }
    return diffListOperations(
        newItems = dated,
        existingIds = rows.events.mapTo(mutableSetOf()) { it.dataId },
        dataIdOf = { (event, _) -> event.dataId },
        insert = { (event, date) ->
            dataInsert(Event.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(contactEventValues(event, date))
        },
        update = { dataId, (event, date) ->
            dataUpdate(dataId).withValues(contactEventValues(event, date))
        },
    )
}

internal fun nicknameValues(nickname: String): ContentValues =
    ContentValues().apply {
        put(Nickname.NAME, nickname)
        put(Nickname.TYPE, Nickname.TYPE_DEFAULT)
    }

/** Никнейм — одиночное поле: строка обновляется, удаляется или вставляется целиком. */
internal fun nicknameOperations(
    rawContactId: Long,
    existing: ExistingNickname?,
    nickname: String,
): List<ContentProviderOperation> =
    when {
        nickname.isBlank() -> existing?.let { listOf(deleteDataOperation(it.dataId)) }.orEmpty()
        existing == null ->
            listOf(
                dataInsert(Nickname.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                    .withValues(nicknameValues(nickname))
                    .build(),
            )
        existing.nickname == nickname -> emptyList()
        else -> listOf(dataUpdate(existing.dataId).withValues(nicknameValues(nickname)).build())
    }
