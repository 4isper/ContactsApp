package com.m4isper.contactsapp.presentation.editor.edits

import com.m4isper.contactsapp.domain.model.contact.ImProtocol
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.withRow

/** Правки формы: сайты и мессенджеры. */
internal fun EditorUiState.withWebsite(
    rowId: Long,
    value: String,
): EditorUiState = copy(websites = websites.withRow(rowId) { website -> website.copy(url = value) })

internal fun EditorUiState.withWebsiteKind(
    rowId: Long,
    kind: WebsiteKind,
): EditorUiState = copy(websites = websites.withRow(rowId) { website -> website.copy(kind = kind) })

internal fun EditorUiState.withWebsiteLabel(
    rowId: Long,
    value: String,
): EditorUiState = copy(websites = websites.withRow(rowId) { website -> website.copy(customLabel = value) })

internal fun EditorUiState.withIm(
    rowId: Long,
    value: String,
): EditorUiState = copy(instantMessages = instantMessages.withRow(rowId) { message -> message.copy(address = value) })

internal fun EditorUiState.withImProtocol(
    rowId: Long,
    protocol: ImProtocol,
): EditorUiState =
    copy(instantMessages = instantMessages.withRow(rowId) { message -> message.copy(protocol = protocol) })

internal fun EditorUiState.withImCustomProtocol(
    rowId: Long,
    value: String,
): EditorUiState =
    copy(instantMessages = instantMessages.withRow(rowId) { message -> message.copy(customProtocol = value) })

internal fun EditorUiState.withSip(
    rowId: Long,
    value: String,
): EditorUiState = copy(sipAddresses = sipAddresses.withRow(rowId) { sip -> sip.copy(address = value) })

internal fun EditorUiState.withSipKind(
    rowId: Long,
    kind: SipKind,
): EditorUiState = copy(sipAddresses = sipAddresses.withRow(rowId) { sip -> sip.copy(kind = kind) })

internal fun EditorUiState.withSipLabel(
    rowId: Long,
    value: String,
): EditorUiState = copy(sipAddresses = sipAddresses.withRow(rowId) { sip -> sip.copy(customLabel = value) })
