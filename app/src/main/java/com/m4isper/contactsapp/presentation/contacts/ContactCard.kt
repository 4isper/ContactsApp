package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.ui.components.ListAvatar
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

private val QuickActionSize = 40.dp

@Composable
internal fun ContactCard(
    contact: ContactDetails,
    isFavorite: Boolean,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    density: ListDensity = ListDensity.COMFORTABLE,
    onCall: (() -> Unit)? = null,
    onSms: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val call = onCall?.takeIf { contact.phones.isNotEmpty() }
    val sms = onSms?.takeIf { contact.phones.isNotEmpty() }

    if (isSelectionMode || (call == null && sms == null)) {
        ContactCardSurface(
            contact = contact,
            isFavorite = isFavorite,
            selected = selected,
            isSelectionMode = isSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            density = density,
            onCall = call,
            onSms = sms,
            onToggleFavorite = onToggleFavorite,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.m, vertical = spacing.xs),
        )
        return
    }

    val swipeState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val currentCall by rememberUpdatedState(call)
    val currentSms by rememberUpdatedState(sms)
    val onSwipeDismiss =
        remember(swipeState, scope) {
            { direction: SwipeToDismissBoxValue ->
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> currentSms?.invoke()
                    SwipeToDismissBoxValue.EndToStart -> currentCall?.invoke()
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                scope.launch { swipeState.reset() }
                Unit
            }
        }
    SwipeToDismissBox(
        state = swipeState,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
        enableDismissFromStartToEnd = sms != null,
        enableDismissFromEndToStart = call != null,
        onDismiss = onSwipeDismiss,
        backgroundContent = {
            SwipeActionBackground(
                direction = swipeState.dismissDirection,
                modifier = Modifier.fillMaxSize(),
            )
        },
    ) {
        ContactCardSurface(
            contact = contact,
            isFavorite = isFavorite,
            selected = selected,
            isSelectionMode = isSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            density = density,
            onCall = call,
            onSms = sms,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun ContactCardSurface(
    contact: ContactDetails,
    isFavorite: Boolean,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    density: ListDensity,
    onCall: (() -> Unit)?,
    onSms: (() -> Unit)?,
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val compact = density == ListDensity.COMPACT
    var menuOpen by remember { mutableStateOf(false) }
    val hasCallOrSms = onCall != null || onSms != null

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
            modifier
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
            } else {
                FavoriteQuickAction(
                    contactId = contact.contactId,
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite,
                )
                if (hasCallOrSms) {
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier =
                                Modifier
                                    .size(QuickActionSize)
                                    .testTag("quick_actions_${contact.contactId}"),
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.action_more),
                            )
                        }
                        QuickActionsMenu(
                            contactId = contact.contactId,
                            expanded = menuOpen,
                            onDismiss = { menuOpen = false },
                            onCall = onCall,
                            onSms = onSms,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteQuickAction(
    contactId: Long,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
) {
    val spacing = LocalSpacing.current
    if (onToggleFavorite == null) {
        if (isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(spacing.xl),
            )
        }
        return
    }
    IconButton(
        onClick = onToggleFavorite,
        modifier =
            Modifier
                .size(QuickActionSize)
                .testTag("quick_favorite_$contactId"),
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription =
                stringResource(
                    if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                ),
            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(spacing.xl),
        )
    }
}

@Composable
private fun QuickActionsMenu(
    contactId: Long,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCall: (() -> Unit)?,
    onSms: (() -> Unit)?,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (onCall != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_call)) },
                onClick = {
                    onDismiss()
                    onCall()
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag("quick_call_$contactId"),
            )
        }
        if (onSms != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_sms)) },
                onClick = {
                    onDismiss()
                    onSms()
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag("quick_sms_$contactId"),
            )
        }
    }
}

@Composable
private fun SwipeActionBackground(
    direction: SwipeToDismissBoxValue,
    modifier: Modifier = Modifier,
) {
    if (direction == SwipeToDismissBoxValue.Settled) return
    val spacing = LocalSpacing.current
    val isCall = direction == SwipeToDismissBoxValue.EndToStart
    val color = if (isCall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Row(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(color.copy(alpha = 0.16f))
                .padding(horizontal = spacing.xl),
        horizontalArrangement = if (isCall) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isCall) Icons.Filled.Call else Icons.AutoMirrored.Filled.Message,
            contentDescription =
                stringResource(
                    if (isCall) R.string.action_call else R.string.action_send_sms,
                ),
            tint = color,
        )
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
