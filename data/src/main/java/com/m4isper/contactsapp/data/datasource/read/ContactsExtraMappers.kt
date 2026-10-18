package com.m4isper.contactsapp.data.datasource.read

import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Relation
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactEvent
import com.m4isper.contactsapp.domain.model.ContactRelation
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.RelationKind

/**
 * Тип связи лежит в data2; константы провайдера сопоставляются с [RelationKind]
 * через таблицу, а не через when: ветвей больше лимита CyclomaticComplexMethod.
 */
private val RELATION_KIND_BY_TYPE =
    mapOf(
        Relation.TYPE_ASSISTANT to RelationKind.ASSISTANT,
        Relation.TYPE_BROTHER to RelationKind.BROTHER,
        Relation.TYPE_CHILD to RelationKind.CHILD,
        Relation.TYPE_DOMESTIC_PARTNER to RelationKind.DOMESTIC_PARTNER,
        Relation.TYPE_FATHER to RelationKind.FATHER,
        Relation.TYPE_FRIEND to RelationKind.FRIEND,
        Relation.TYPE_MANAGER to RelationKind.MANAGER,
        Relation.TYPE_MOTHER to RelationKind.MOTHER,
        Relation.TYPE_PARENT to RelationKind.PARENT,
        Relation.TYPE_PARTNER to RelationKind.PARTNER,
        Relation.TYPE_REFERRED_BY to RelationKind.REFERRED_BY,
        Relation.TYPE_RELATIVE to RelationKind.RELATIVE,
        Relation.TYPE_SISTER to RelationKind.SISTER,
        Relation.TYPE_SPOUSE to RelationKind.SPOUSE,
    )

/** Неизвестный или отсутствующий тип трактуем как произвольную связь. */
internal fun mapRelationKind(type: Int): RelationKind = RELATION_KIND_BY_TYPE[type] ?: RelationKind.OTHER

/** Обратное сопоставление: все виды, кроме произвольного, есть в таблице. */
internal fun relationTypeInt(kind: RelationKind): Int =
    RELATION_KIND_BY_TYPE.entries
        .firstOrNull { it.value == kind }
        ?.key
        ?: Relation.TYPE_CUSTOM

internal fun relationsOf(rows: List<DataRow>): List<ContactRelation> =
    rows
        .filter { it.mimetype == Relation.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            row.data1?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                ContactRelation(
                    name = name,
                    kind = mapRelationKind(row.data2Int),
                    customLabel = customLabelOf(row.data2Int, row.data3, Relation.TYPE_CUSTOM),
                )
            }
        }.distinctBy { it.name }

/** День рождения читается отдельно от прочих событий, поэтому его тип здесь не встречается. */
internal fun mapEventKind(type: Int): EventKind =
    when (type) {
        Event.TYPE_ANNIVERSARY -> EventKind.ANNIVERSARY
        else -> EventKind.OTHER
    }

internal fun eventsOf(rows: List<DataRow>): List<ContactEvent> =
    rows
        .filter { it.mimetype == Event.CONTENT_ITEM_TYPE && it.data2Int != Event.TYPE_BIRTHDAY }
        .mapNotNull { row ->
            Birthday.parse(row.data1)?.let { date ->
                ContactEvent(
                    date = date,
                    kind = mapEventKind(row.data2Int),
                    customLabel = customLabelOf(row.data2Int, row.data3, Event.TYPE_CUSTOM),
                )
            }
        }

internal fun nicknameOf(rows: List<DataRow>): String? =
    rows
        .firstOrNull { it.mimetype == Nickname.CONTENT_ITEM_TYPE }
        ?.data1
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/**
 * Событие в форме редактора: неразобранная дата сохраняется как есть —
 * чтобы строка не исчезла из формы и не была удалена при сохранении.
 */
internal fun editableEventOf(row: ExistingEvent): EditableEvent {
    val date = Birthday.parse(row.raw)
    return EditableEvent(
        dataId = row.dataId,
        date = date,
        kind = mapEventKind(row.type),
        customLabel = customLabelOf(row.type, row.label, Event.TYPE_CUSTOM),
        unparsedDate = row.raw.takeIf { date == null },
    )
}
