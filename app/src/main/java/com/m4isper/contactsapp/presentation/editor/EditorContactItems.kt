package com.m4isper.contactsapp.presentation.editor

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Строки каналов связи: телефоны, почта, мессенджеры, адреса и сайты. */
internal fun LazyListScope.editorContactItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    phoneItems(uiState, viewModel)
    emailItems(uiState, viewModel)
    instantMessageItems(uiState, viewModel)
    addressItems(uiState, viewModel)
    websiteItems(uiState, viewModel)
}

private fun LazyListScope.phoneItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    items(
        items = uiState.phones,
        key = { it.rowId },
    ) { row ->
        PhoneEditorRow(
            row = row,
            canRemove = uiState.phones.size > 1,
            onPhoneChange = viewModel::onPhoneChange,
            onKindChange = viewModel::onKindChange,
            onCustomLabelChange = viewModel::onCustomLabelChange,
            onRemove = viewModel::onRemovePhone,
        )
    }
    item(key = "add-phone") {
        val spacing = LocalSpacing.current
        OutlinedButton(
            onClick = viewModel::onAddPhone,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.m, vertical = spacing.s)
                    .testTag("editor_add_phone_button"),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(spacing.s))
            Text(stringResource(R.string.editor_add_phone))
        }
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
    items(
        items = uiState.emails,
        key = { it.rowId },
    ) { row ->
        EmailEditorRow(
            row = row,
            onEmailChange = viewModel::onEmailChange,
            onKindChange = viewModel::onEmailKindChange,
            onCustomLabelChange = viewModel::onEmailCustomLabelChange,
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
            onAddressChange = viewModel::onAddressChange,
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
            onRemove = viewModel::onRemoveWebsite,
        )
    }
}
