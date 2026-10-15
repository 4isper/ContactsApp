package com.m4isper.contactsapp.presentation.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R

@Composable
internal fun ConfirmCallDialog(
    contactName: String,
    contactPhone: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contactName) },
        text = { Text(stringResource(R.string.phone_label_mobile, contactPhone)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_call_button"),
            ) {
                Text(stringResource(R.string.action_call))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_call_button"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
