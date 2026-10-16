package com.m4isper.contactsapp.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.components.ConfirmDialog

@Composable
internal fun ConfirmCallDialog(
    contactName: String,
    contactPhone: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = contactName,
        text = stringResource(R.string.phone_label_mobile, contactPhone),
        confirmLabel = stringResource(R.string.action_call),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmTestTag = "confirm_call_button",
        dismissTestTag = "dismiss_call_button",
    )
}
