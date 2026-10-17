package com.m4isper.contactsapp.presentation.common

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactEvent
import com.m4isper.contactsapp.domain.model.ContactRelation
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.RelationKind
import java.util.Locale

/** TYPE связи: отдельного стандарта нет, используем имена RelationKind. */
internal fun vcardRelationType(kind: RelationKind): String = kind.name

/** Вид связи: неизвестный или произвольный токен трактуем как OTHER. */
internal fun relationKindOfVCard(types: Set<String>): RelationKind =
    types
        .map { it.trim().replace(' ', '_').uppercase(Locale.ROOT) }
        .firstNotNullOfOrNull { token -> RelationKind.entries.firstOrNull { it.name == token } }
        ?: RelationKind.OTHER

/** Произвольная метка из параметра X-ABLabel (формат Apple/Android). */
internal fun vcardLabelOf(params: Map<String, String>): String? =
    params["X-ABLABEL"]?.trim()?.takeIf { it.isNotEmpty() }

/** Связь: RELATED с TYPE; метка произвольного вида — в X-ABLabel. */
internal fun vcardRelationProperty(relation: ContactRelation): String {
    val label = relation.customLabel?.takeIf { relation.kind == RelationKind.OTHER && it.isNotBlank() }
    val head =
        buildString {
            append("RELATED;TYPE=").append(vcardRelationType(relation.kind))
            label?.let { append(";X-ABLabel=").append(it) }
        }
    return "$head:${escapeVCard(relation.name)}"
}

/** Годовщина — стандартное ANNIVERSARY, прочие даты — X-ABDATE (формат Apple/Android). */
internal fun vcardEventProperty(event: ContactEvent): String {
    val name = if (event.kind == EventKind.ANNIVERSARY) "ANNIVERSARY" else "X-ABDATE"
    val label = event.customLabel?.takeIf { event.kind == EventKind.OTHER && it.isNotBlank() }
    val head = if (label == null) name else "$name;X-ABLabel=$label"
    return "$head:${event.date.toStorageString()}"
}

/** Расширенные свойства экспорта: никнейм, связи и события. */
internal fun vcardExtraLines(details: ContactDetails): List<String> =
    buildList {
        details.nickname?.let { add("NICKNAME:${escapeVCard(it)}") }
        details.relations.forEach { add(vcardRelationProperty(it)) }
        details.events.forEach { add(vcardEventProperty(it)) }
    }
