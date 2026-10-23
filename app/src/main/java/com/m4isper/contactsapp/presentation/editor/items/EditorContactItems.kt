package com.m4isper.contactsapp.presentation.editor.items

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.domain.model.contact.FormField
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.components.SectionHeader
import com.m4isper.contactsapp.presentation.editor.edits.withAddedAddress
import com.m4isper.contactsapp.presentation.editor.edits.withAddedEmail
import com.m4isper.contactsapp.presentation.editor.edits.withAddedIm
import com.m4isper.contactsapp.presentation.editor.edits.withAddedPhone
import com.m4isper.contactsapp.presentation.editor.edits.withAddedSip
import com.m4isper.contactsapp.presentation.editor.edits.withAddedWebsite
import com.m4isper.contactsapp.presentation.editor.edits.withAddressCity
import com.m4isper.contactsapp.presentation.editor.edits.withAddressCountry
import com.m4isper.contactsapp.presentation.editor.edits.withAddressKind
import com.m4isper.contactsapp.presentation.editor.edits.withAddressLabel
import com.m4isper.contactsapp.presentation.editor.edits.withAddressNeighborhood
import com.m4isper.contactsapp.presentation.editor.edits.withAddressPoBox
import com.m4isper.contactsapp.presentation.editor.edits.withAddressPostcode
import com.m4isper.contactsapp.presentation.editor.edits.withAddressRegion
import com.m4isper.contactsapp.presentation.editor.edits.withAddressStreet
import com.m4isper.contactsapp.presentation.editor.edits.withEmail
import com.m4isper.contactsapp.presentation.editor.edits.withEmailKind
import com.m4isper.contactsapp.presentation.editor.edits.withEmailLabel
import com.m4isper.contactsapp.presentation.editor.edits.withIm
import com.m4isper.contactsapp.presentation.editor.edits.withImCustomProtocol
import com.m4isper.contactsapp.presentation.editor.edits.withImProtocol
import com.m4isper.contactsapp.presentation.editor.edits.withMovedEmail
import com.m4isper.contactsapp.presentation.editor.edits.withMovedPhone
import com.m4isper.contactsapp.presentation.editor.edits.withPhone
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneKind
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneLabel
import com.m4isper.contactsapp.presentation.editor.edits.withPrimaryEmail
import com.m4isper.contactsapp.presentation.editor.edits.withPrimaryPhone
import com.m4isper.contactsapp.presentation.editor.edits.withSip
import com.m4isper.contactsapp.presentation.editor.edits.withSipKind
import com.m4isper.contactsapp.presentation.editor.edits.withSipLabel
import com.m4isper.contactsapp.presentation.editor.edits.withWebsite
import com.m4isper.contactsapp.presentation.editor.edits.withWebsiteKind
import com.m4isper.contactsapp.presentation.editor.edits.withWebsiteLabel
import com.m4isper.contactsapp.presentation.editor.edits.withoutAddress
import com.m4isper.contactsapp.presentation.editor.edits.withoutEmail
import com.m4isper.contactsapp.presentation.editor.edits.withoutIm
import com.m4isper.contactsapp.presentation.editor.edits.withoutPhone
import com.m4isper.contactsapp.presentation.editor.edits.withoutSip
import com.m4isper.contactsapp.presentation.editor.edits.withoutWebsite
import com.m4isper.contactsapp.presentation.editor.fieldMessage
import com.m4isper.contactsapp.presentation.editor.rows.AddressEditorRow
import com.m4isper.contactsapp.presentation.editor.rows.EmailEditorRow
import com.m4isper.contactsapp.presentation.editor.rows.ImEditorRow
import com.m4isper.contactsapp.presentation.editor.rows.PhoneEditorRow
import com.m4isper.contactsapp.presentation.editor.rows.SipEditorRow
import com.m4isper.contactsapp.presentation.editor.rows.WebsiteEditorRow

/** Строки каналов связи: телефоны, почта, мессенджеры, SIP, адреса и сайты. */
internal fun LazyListScope.editorContactItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    phoneItems(uiState, edit)
    emailItems(uiState, edit)
    instantMessageItems(uiState, edit)
    sipItems(uiState, edit)
    addressItems(uiState, edit)
    websiteItems(uiState, edit)
}

private fun LazyListScope.phoneItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "phones-header") {
        SectionHeader(
            titleRes = R.string.editor_section_phones,
            onAdd = { edit { it.withAddedPhone() } },
            addLabelRes = R.string.editor_add_phone,
            addTestTag = "editor_add_phone_button",
        )
    }
    itemsIndexed(
        items = uiState.phones,
        key = { _, row -> row.rowId },
    ) { index, row ->
        PhoneEditorRow(
            row = row,
            error = uiState.fieldErrors[FormField.PHONE]?.fieldMessage(),
            canRemove = uiState.phones.size > 1,
            canMoveUp = index > 0,
            canMoveDown = index < uiState.phones.lastIndex,
            onPhoneChange = { rowId, value -> edit { it.withPhone(rowId, value) } },
            onKindChange = { rowId, kind -> edit { it.withPhoneKind(rowId, kind) } },
            onCustomLabelChange = { rowId, value -> edit { it.withPhoneLabel(rowId, value) } },
            onSetPrimary = { rowId -> edit { it.withPrimaryPhone(rowId) } },
            onMoveUp = { rowId -> edit { it.withMovedPhone(rowId, -1) } },
            onMoveDown = { rowId -> edit { it.withMovedPhone(rowId, 1) } },
            onRemove = { rowId -> edit { it.withoutPhone(rowId) } },
        )
    }
}

private fun LazyListScope.emailItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "emails-header") {
        SectionHeader(
            titleRes = R.string.editor_section_emails,
            onAdd = { edit { it.withAddedEmail() } },
            addLabelRes = R.string.editor_add_email,
            addTestTag = "editor_add_email_button",
        )
    }
    itemsIndexed(
        items = uiState.emails,
        key = { _, row -> row.rowId },
    ) { index, row ->
        EmailEditorRow(
            row = row,
            error = uiState.fieldErrors[FormField.EMAIL]?.fieldMessage(),
            canMoveUp = index > 0,
            canMoveDown = index < uiState.emails.lastIndex,
            onEmailChange = { rowId, value -> edit { it.withEmail(rowId, value) } },
            onKindChange = { rowId, kind -> edit { it.withEmailKind(rowId, kind) } },
            onCustomLabelChange = { rowId, value -> edit { it.withEmailLabel(rowId, value) } },
            onSetPrimary = { rowId -> edit { it.withPrimaryEmail(rowId) } },
            onMoveUp = { rowId -> edit { it.withMovedEmail(rowId, -1) } },
            onMoveDown = { rowId -> edit { it.withMovedEmail(rowId, 1) } },
            onRemove = { rowId -> edit { it.withoutEmail(rowId) } },
        )
    }
}

private fun LazyListScope.instantMessageItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "instant-messages-header") {
        SectionHeader(
            titleRes = R.string.editor_section_instant_messages,
            onAdd = { edit { it.withAddedIm() } },
            addLabelRes = R.string.editor_add_im,
            addTestTag = "editor_add_im_button",
        )
    }
    items(
        items = uiState.instantMessages,
        key = { it.rowId },
    ) { row ->
        ImEditorRow(
            row = row,
            onImChange = { rowId, value -> edit { it.withIm(rowId, value) } },
            onProtocolChange = {
                rowId,
                protocol,
                ->
                edit { it.withImProtocol(rowId, protocol) }
            },
            onCustomProtocolChange = {
                rowId,
                value,
                ->
                edit { it.withImCustomProtocol(rowId, value) }
            },
            onRemove = { rowId -> edit { it.withoutIm(rowId) } },
        )
    }
}

private fun LazyListScope.sipItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "sip-header") {
        SectionHeader(
            titleRes = R.string.editor_section_sip,
            onAdd = { edit { it.withAddedSip() } },
            addLabelRes = R.string.editor_add_sip,
            addTestTag = "editor_add_sip_button",
        )
    }
    items(
        items = uiState.sipAddresses,
        key = { it.rowId },
    ) { row ->
        SipEditorRow(
            row = row,
            onSipChange = { rowId, value -> edit { it.withSip(rowId, value) } },
            onKindChange = { rowId, kind -> edit { it.withSipKind(rowId, kind) } },
            onCustomLabelChange = { rowId, value -> edit { it.withSipLabel(rowId, value) } },
            onRemove = { rowId -> edit { it.withoutSip(rowId) } },
        )
    }
}

private fun LazyListScope.addressItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "addresses-header") {
        SectionHeader(
            titleRes = R.string.editor_section_addresses,
            onAdd = { edit { it.withAddedAddress() } },
            addLabelRes = R.string.editor_add_address,
            addTestTag = "editor_add_address_button",
        )
    }
    items(
        items = uiState.addresses,
        key = { it.rowId },
    ) { row ->
        addressRow(row = row, edit = edit)
    }
}

/** Строка адреса: у неё больше полей, чем у остальных строк формы. */
@Composable
private fun addressRow(
    row: EditorRow<EditableAddress>,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    AddressEditorRow(
        row = row,
        onStreetChange = { rowId, value -> edit { it.withAddressStreet(rowId, value) } },
        onPoBoxChange = { rowId, value -> edit { it.withAddressPoBox(rowId, value) } },
        onNeighborhoodChange = { rowId, value -> edit { it.withAddressNeighborhood(rowId, value) } },
        onCityChange = { rowId, value -> edit { it.withAddressCity(rowId, value) } },
        onRegionChange = { rowId, value -> edit { it.withAddressRegion(rowId, value) } },
        onPostcodeChange = { rowId, value -> edit { it.withAddressPostcode(rowId, value) } },
        onCountryChange = { rowId, value -> edit { it.withAddressCountry(rowId, value) } },
        onKindChange = { rowId, kind -> edit { it.withAddressKind(rowId, kind) } },
        onCustomLabelChange = { rowId, value -> edit { it.withAddressLabel(rowId, value) } },
        onRemove = { rowId -> edit { it.withoutAddress(rowId) } },
    )
}

private fun LazyListScope.websiteItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "websites-header") {
        SectionHeader(
            titleRes = R.string.editor_section_websites,
            onAdd = { edit { it.withAddedWebsite() } },
            addLabelRes = R.string.editor_add_website,
            addTestTag = "editor_add_website_button",
        )
    }
    items(
        items = uiState.websites,
        key = { it.rowId },
    ) { row ->
        WebsiteEditorRow(
            row = row,
            error = uiState.fieldErrors[FormField.WEBSITE]?.fieldMessage(),
            onWebsiteChange = { rowId, value -> edit { it.withWebsite(rowId, value) } },
            onKindChange = { rowId, kind -> edit { it.withWebsiteKind(rowId, kind) } },
            onCustomLabelChange = {
                rowId,
                value,
                ->
                edit { it.withWebsiteLabel(rowId, value) }
            },
            onRemove = { rowId -> edit { it.withoutWebsite(rowId) } },
        )
    }
}
