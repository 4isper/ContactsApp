package com.m4isper.contactsapp.presentation.editor.edits

import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.moved
import com.m4isper.contactsapp.presentation.editor.withRow

/** Правки формы: почта. */
internal fun EditorUiState.withEmail(
    rowId: Long,
    value: String,
): EditorUiState = copy(emails = emails.withRow(rowId) { email -> email.copy(address = value) })

internal fun EditorUiState.withEmailKind(
    rowId: Long,
    kind: EmailKind,
): EditorUiState = copy(emails = emails.withRow(rowId) { email -> email.copy(kind = kind) })

internal fun EditorUiState.withEmailLabel(
    rowId: Long,
    value: String,
): EditorUiState = copy(emails = emails.withRow(rowId) { email -> email.copy(customLabel = value) })

internal fun EditorUiState.withPrimaryEmail(rowId: Long): EditorUiState =
    copy(
        emails =
            emails.map { row ->
                row.copy(value = row.value.copy(isPrimary = row.rowId == rowId))
            },
    )

internal fun EditorUiState.withMovedEmail(
    rowId: Long,
    offset: Int,
): EditorUiState = copy(emails = emails.moved(rowId, offset))
