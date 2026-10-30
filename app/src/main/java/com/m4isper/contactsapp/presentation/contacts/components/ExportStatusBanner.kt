package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.contacts.effects.ExportSummary
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Ход и итог экспорта прямо над списком: прогресс виден, пока пишется файл,
 * а итог остаётся на экране, пока его не закроют.
 */
@Composable
internal fun ExportStatusBanner(
    inProgress: Boolean,
    summary: ExportSummary?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    when {
        inProgress ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m, vertical = spacing.s)
                        .testTag("export_progress"),
            ) {
                Text(
                    text = stringResource(R.string.export_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(spacing.xl),
                )
            }
        summary != null ->
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (summary.failed) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                    ),
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m, vertical = spacing.xs)
                        .testTag("export_summary"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = spacing.m),
                ) {
                    Text(
                        text = summary.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (summary.failed) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("export_summary_close"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint =
                                if (summary.failed) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
    }
}
