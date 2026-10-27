package com.m4isper.contactsapp.presentation.editor.edits

import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.withRow

/** Правки формы: адреса. */
internal fun EditorUiState.withAddress(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(address = value) })

internal fun EditorUiState.withAddressStreet(
    rowId: Long,
    value: String,
): EditorUiState =
    copy(
        addresses =
            addresses.withRow(rowId) { address ->
                address.copy(street = value, address = "")
            },
    )

internal fun EditorUiState.withAddressPoBox(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(poBox = value) })

internal fun EditorUiState.withAddressNeighborhood(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(neighborhood = value) })

internal fun EditorUiState.withAddressCity(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(city = value) })

internal fun EditorUiState.withAddressRegion(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(region = value) })

internal fun EditorUiState.withAddressPostcode(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(postcode = value) })

internal fun EditorUiState.withAddressCountry(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(country = value) })

internal fun EditorUiState.withAddressKind(
    rowId: Long,
    kind: AddressKind,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(kind = kind) })

internal fun EditorUiState.withAddressLabel(
    rowId: Long,
    value: String,
): EditorUiState = copy(addresses = addresses.withRow(rowId) { address -> address.copy(customLabel = value) })
