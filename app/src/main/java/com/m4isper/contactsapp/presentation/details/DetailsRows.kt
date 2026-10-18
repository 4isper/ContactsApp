package com.m4isper.contactsapp.presentation.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.presentation.common.format.phoneKindLabel
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun DataRow(
    primary: String,
    secondary: String?,
    actions: List<RowAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primary,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (secondary != null) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Icon(
                        action.icon,
                        contentDescription = action.contentDescription,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PhoneRow(
    phone: PhoneNumber,
    onClick: () -> Unit,
    onSmsClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .testTag("phone_row_${phone.number}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phone.number,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = phone.customLabel ?: phoneKindLabel(phone.kind),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSmsClick) {
                Icon(
                    Icons.Filled.Message,
                    contentDescription = stringResource(R.string.action_send_sms),
                )
            }
            IconButton(onClick = onCopyClick) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy),
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = stringResource(R.string.action_call),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
