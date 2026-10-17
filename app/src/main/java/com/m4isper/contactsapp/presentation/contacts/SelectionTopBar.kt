package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R

/** Контекстная панель режима выделения: счётчик, «выбрать все», шаринг и удаление. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopBar(
    count: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = pluralStringResource(R.plurals.selection_count, count, count),
                style = MaterialTheme.typography.titleLarge,
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
                onClick = onShare,
                modifier = Modifier.testTag("share_selected_button"),
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.action_share),
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_selected_button"),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
        },
        modifier = Modifier.testTag("selection_top_bar"),
    )
}
