package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.ui.components.ListAvatar
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun ContactCard(
    contact: ContactDetails,
    isFavorite: Boolean,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    density: ListDensity = ListDensity.COMFORTABLE,
) {
    val spacing = LocalSpacing.current
    val compact = density == ListDensity.COMPACT
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .clip(MaterialTheme.shapes.medium)
                .testTag("contact_row_${contact.contactId}")
                .then(
                    if (isSelectionMode) {
                        Modifier.selectable(
                            selected = selected,
                            role = Role.Checkbox,
                            onClick = onClick,
                        )
                    } else {
                        Modifier.combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                    },
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(
                    horizontal = spacing.m,
                    vertical = if (compact) spacing.xs else spacing.m,
                ),
        ) {
            ListAvatar(
                name = contact.name,
                photoUri = contact.photoUri,
                size = if (compact) spacing.avatarList - spacing.s else spacing.avatarList,
            )

            Spacer(modifier = Modifier.width(spacing.m))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contactSubtitle(contact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                )
            } else if (isFavorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(spacing.xl),
                )
            }
        }
    }
}

@Composable
internal fun contactSubtitle(contact: ContactDetails): String =
    if (contact.phones.isEmpty()) {
        contact.emails
            .firstOrNull()
            ?.address
            .orEmpty()
    } else {
        val primary = contact.phones.first().number
        if (contact.phones.size <= 1) {
            primary
        } else {
            "$primary · " +
                pluralStringResource(
                    R.plurals.phone_count_extra,
                    contact.phones.size - 1,
                    contact.phones.size - 1,
                )
        }
    }
