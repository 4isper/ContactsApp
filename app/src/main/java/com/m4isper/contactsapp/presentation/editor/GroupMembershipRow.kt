package com.m4isper.contactsapp.presentation.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Строка «Группы»: выбранные метки и вход в пикер. */
@Composable
internal fun GroupMembershipRow(
    selected: List<ContactGroup>,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .testTag("editor_groups_row"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    if (selected.isEmpty()) {
                        stringResource(R.string.editor_groups_empty)
                    } else {
                        selected.joinToString(", ") { it.title }
                    },
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (selected.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.s),
            )
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.editor_groups_edit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
