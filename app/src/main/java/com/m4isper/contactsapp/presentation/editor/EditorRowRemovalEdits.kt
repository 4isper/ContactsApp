package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.EditablePhone

/** Правки формы: удаление строк. */
internal fun EditorUiState.withoutPhone(rowId: Long): EditorUiState {
    val remaining = phones.filterNot { row -> row.rowId == rowId }
    return copy(
        phones = remaining.ifEmpty { listOf(EditorRow(rowId = nextRowId(), value = EditablePhone(number = ""))) },
    )
}

internal fun EditorUiState.withoutEmail(rowId: Long): EditorUiState =
    copy(emails = emails.filterNot { row -> row.rowId == rowId })

internal fun EditorUiState.withoutAddress(rowId: Long): EditorUiState =
    copy(addresses = addresses.filterNot { row -> row.rowId == rowId })

internal fun EditorUiState.withoutWebsite(rowId: Long): EditorUiState =
    copy(websites = websites.filterNot { row -> row.rowId == rowId })

internal fun EditorUiState.withoutIm(rowId: Long): EditorUiState =
    copy(instantMessages = instantMessages.filterNot { row -> row.rowId == rowId })

internal fun EditorUiState.withoutSip(rowId: Long): EditorUiState =
    copy(sipAddresses = sipAddresses.filterNot { row -> row.rowId == rowId })

internal fun EditorUiState.withoutRelation(rowId: Long): EditorUiState =
    copy(relations = relations.filterNot { row -> row.rowId == rowId })

internal fun EditorUiState.withoutEvent(rowId: Long): EditorUiState =
    copy(events = events.filterNot { row -> row.rowId == rowId })
