package com.m4isper.contactsapp.presentation.editor

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.m4isper.contactsapp.R

/** Строки каналов связи: телефоны, почта, мессенджеры, SIP, адреса и сайты. */
internal fun LazyListScope.editorContactItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    phoneItems(uiState, viewModel)
    emailItems(uiState, viewModel)
    instantMessageItems(uiState, viewModel)
    sipItems(uiState, viewModel)
    addressItems(uiState, viewModel)
    websiteItems(uiState, viewModel)
}

private fun LazyListScope.phoneItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "phones-header") {
        SectionHeader(
            titleRes = R.string.editor_section_phones,
            onAdd = viewModel::onAddPhone,
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
            canRemove = uiState.phones.size > 1,
            canMoveUp = index > 0,
            canMoveDown = index < uiState.phones.lastIndex,
            onPhoneChange = viewModel::onPhoneChange,
            onKindChange = viewModel::onKindChange,
            onCustomLabelChange = viewModel::onCustomLabelChange,
            onSetPrimary = viewModel::onSetPrimaryPhone,
            onMoveUp = { viewModel.onMovePhone(it, -1) },
            onMoveDown = { viewModel.onMovePhone(it, 1) },
            onRemove = viewModel::onRemovePhone,
        )
    }
}

private fun LazyListScope.emailItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "emails-header") {
        SectionHeader(
            titleRes = R.string.editor_section_emails,
            onAdd = viewModel::onAddEmail,
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
            canMoveUp = index > 0,
            canMoveDown = index < uiState.emails.lastIndex,
            onEmailChange = viewModel::onEmailChange,
            onKindChange = viewModel::onEmailKindChange,
            onCustomLabelChange = viewModel::onEmailCustomLabelChange,
            onSetPrimary = viewModel::onSetPrimaryEmail,
            onMoveUp = { viewModel.onMoveEmail(it, -1) },
            onMoveDown = { viewModel.onMoveEmail(it, 1) },
            onRemove = viewModel::onRemoveEmail,
        )
    }
}

private fun LazyListScope.instantMessageItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "instant-messages-header") {
        SectionHeader(
            titleRes = R.string.editor_section_instant_messages,
            onAdd = viewModel::onAddIm,
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
            onImChange = viewModel::onImChange,
            onProtocolChange = viewModel::onImProtocolChange,
            onCustomProtocolChange = viewModel::onImCustomProtocolChange,
            onRemove = viewModel::onRemoveIm,
        )
    }
}

private fun LazyListScope.sipItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "sip-header") {
        SectionHeader(
            titleRes = R.string.editor_section_sip,
            onAdd = viewModel::onAddSip,
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
            onSipChange = viewModel::onSipChange,
            onKindChange = viewModel::onSipKindChange,
            onCustomLabelChange = viewModel::onSipCustomLabelChange,
            onRemove = viewModel::onRemoveSip,
        )
    }
}

private fun LazyListScope.addressItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "addresses-header") {
        SectionHeader(
            titleRes = R.string.editor_section_addresses,
            onAdd = viewModel::onAddAddress,
            addLabelRes = R.string.editor_add_address,
            addTestTag = "editor_add_address_button",
        )
    }
    items(
        items = uiState.addresses,
        key = { it.rowId },
    ) { row ->
        AddressEditorRow(
            row = row,
            onStreetChange = viewModel::onAddressStreetChange,
            onPoBoxChange = viewModel::onAddressPoBoxChange,
            onNeighborhoodChange = viewModel::onAddressNeighborhoodChange,
            onCityChange = viewModel::onAddressCityChange,
            onRegionChange = viewModel::onAddressRegionChange,
            onPostcodeChange = viewModel::onAddressPostcodeChange,
            onCountryChange = viewModel::onAddressCountryChange,
            onKindChange = viewModel::onAddressKindChange,
            onCustomLabelChange = viewModel::onAddressCustomLabelChange,
            onRemove = viewModel::onRemoveAddress,
        )
    }
}

private fun LazyListScope.websiteItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "websites-header") {
        SectionHeader(
            titleRes = R.string.editor_section_websites,
            onAdd = viewModel::onAddWebsite,
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
            onWebsiteChange = viewModel::onWebsiteChange,
            onKindChange = viewModel::onWebsiteKindChange,
            onCustomLabelChange = viewModel::onWebsiteCustomLabelChange,
            onRemove = viewModel::onRemoveWebsite,
        )
    }
}
