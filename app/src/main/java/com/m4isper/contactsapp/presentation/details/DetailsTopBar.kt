package com.m4isper.contactsapp.presentation.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsTopBar(
    title: String?,
    shareable: ContactDetails?,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onShareAsText: () -> Unit,
    onShareAsQr: () -> Unit,
    onShowQr: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopyToAccount: () -> Unit,
    onMoveToAccount: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                text = title ?: stringResource(R.string.action_back),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            if (shareable != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_button"),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_button"),
                ) {
                    Icon(
                        if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        contentDescription =
                            stringResource(
                                if (isFavorite) {
                                    R.string.action_remove_favorite
                                } else {
                                    R.string.action_add_favorite
                                },
                            ),
                        tint =
                            if (isFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_contact),
                    )
                }
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("details_more_button"),
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_contact_text)) },
                        onClick = {
                            menuExpanded = false
                            onShareAsText()
                        },
                        modifier = Modifier.testTag("share_as_text_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.show_qr)) },
                        onClick = {
                            menuExpanded = false
                            onShowQr()
                        },
                        modifier = Modifier.testTag("show_qr_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_contact_qr)) },
                        onClick = {
                            menuExpanded = false
                            onShareAsQr()
                        },
                        modifier = Modifier.testTag("share_as_qr_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.details_copy_to_account)) },
                        onClick = {
                            menuExpanded = false
                            onCopyToAccount()
                        },
                        modifier = Modifier.testTag("copy_to_account_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.details_move_to_account)) },
                        onClick = {
                            menuExpanded = false
                            onMoveToAccount()
                        },
                        modifier = Modifier.testTag("move_to_account_menu_item"),
                    )
                }
            }
        },
    )
}
