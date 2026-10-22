package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.RelationKind

/** Правки формы: связи и события. */
internal fun EditorUiState.withRelationName(
    rowId: Long,
    value: String,
): EditorUiState = copy(relations = relations.withRow(rowId) { relation -> relation.copy(name = value) })

internal fun EditorUiState.withRelationKind(
    rowId: Long,
    kind: RelationKind,
): EditorUiState = copy(relations = relations.withRow(rowId) { relation -> relation.copy(kind = kind) })

internal fun EditorUiState.withRelationLabel(
    rowId: Long,
    value: String,
): EditorUiState = copy(relations = relations.withRow(rowId) { relation -> relation.copy(customLabel = value) })

internal fun EditorUiState.withEventDate(
    rowId: Long,
    date: Birthday?,
): EditorUiState = copy(events = events.withRow(rowId) { event -> event.copy(date = date) })

internal fun EditorUiState.withEventKind(
    rowId: Long,
    kind: EventKind,
): EditorUiState = copy(events = events.withRow(rowId) { event -> event.copy(kind = kind) })

internal fun EditorUiState.withEventLabel(
    rowId: Long,
    value: String,
): EditorUiState = copy(events = events.withRow(rowId) { event -> event.copy(customLabel = value) })
