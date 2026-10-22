package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.PhoneKind

/** Правки формы: телефоны. */
internal fun EditorUiState.withPhone(
    rowId: Long,
    value: String,
): EditorUiState = copy(phones = phones.withRow(rowId) { phone -> phone.copy(number = value) })

internal fun EditorUiState.withPhoneKind(
    rowId: Long,
    kind: PhoneKind,
): EditorUiState = copy(phones = phones.withRow(rowId) { phone -> phone.copy(kind = kind) })

internal fun EditorUiState.withPhoneLabel(
    rowId: Long,
    value: String,
): EditorUiState = copy(phones = phones.withRow(rowId) { phone -> phone.copy(customLabel = value) })

internal fun EditorUiState.withPrimaryPhone(rowId: Long): EditorUiState =
    copy(
        phones =
            phones.map { row ->
                row.copy(value = row.value.copy(isPrimary = row.rowId == rowId))
            },
    )

internal fun EditorUiState.withMovedPhone(
    rowId: Long,
    offset: Int,
): EditorUiState = copy(phones = phones.moved(rowId, offset))
