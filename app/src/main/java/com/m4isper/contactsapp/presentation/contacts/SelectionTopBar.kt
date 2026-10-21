package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.m4isper.contactsapp.R

/** Контекстная панель режима выделения: счётчик, «выбрать все», добавление в группу, шаринг, экспорт и удаление. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopBar(
    count: Int,
    allSelected: Boolean,
    allFavorite: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToGroup: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = pluralStringResource(R.plurals.selection_count, count, count),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                )
            }
        },
        actions = {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_selected_button"),
            ) {
                Icon(
                    imageVector = if (allFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription =
                        stringResource(
                            if (allFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                        ),
                    tint = if (allFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
                )
            }
            IconButton(
                onClick = onSelectAll,
                modifier = Modifier.testTag("select_all_button"),
            ) {
                Icon(
                    imageVector = if (allSelected) Icons.Filled.Deselect else Icons.Filled.SelectAll,
                    contentDescription =
                        stringResource(
                            if (allSelected) R.string.action_deselect_all else R.string.action_select_all,
                        ),
                )
            }
            IconButton(
                onClick = onAddToGroup,
                modifier = Modifier.testTag("add_to_group_button"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = stringResource(R.string.action_add_to_group),
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.testTag("share_selected_button"),
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.action_share),
                )
            }
            IconButton(
                onClick = onExport,
                modifier = Modifier.testTag("export_selected_button"),
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = stringResource(R.string.action_export),
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_selected_button"),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        modifier = Modifier.testTag("selection_top_bar"),
    )
}
