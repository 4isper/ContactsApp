package com.m4isper.contactsapp.presentation.editor.edits

import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.EditableSipAddress
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.nextRowId

/** Правки формы: добавление строк. */
internal fun EditorUiState.withAddedPhone(): EditorUiState =
    copy(phones = phones + EditorRow(rowId = nextRowId(), value = EditablePhone(number = "")))

internal fun EditorUiState.withAddedEmail(): EditorUiState =
    copy(emails = emails + EditorRow(rowId = nextRowId(), value = EditableEmail(address = "")))

internal fun EditorUiState.withAddedAddress(): EditorUiState =
    copy(addresses = addresses + EditorRow(rowId = nextRowId(), value = EditableAddress(address = "")))

internal fun EditorUiState.withAddedWebsite(): EditorUiState =
    copy(websites = websites + EditorRow(rowId = nextRowId(), value = EditableWebsite(url = "")))

internal fun EditorUiState.withAddedIm(): EditorUiState =
    copy(
        instantMessages =
            instantMessages + EditorRow(rowId = nextRowId(), value = EditableInstantMessage(address = "")),
    )

internal fun EditorUiState.withAddedSip(): EditorUiState =
    copy(sipAddresses = sipAddresses + EditorRow(rowId = nextRowId(), value = EditableSipAddress(address = "")))

internal fun EditorUiState.withAddedRelation(): EditorUiState =
    copy(relations = relations + EditorRow(rowId = nextRowId(), value = EditableRelation(name = "")))

internal fun EditorUiState.withAddedEvent(): EditorUiState =
    copy(events = events + EditorRow(rowId = nextRowId(), value = EditableEvent()))
