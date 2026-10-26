package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.BrandIcons

/**
 * Контекстная панель режима выделения: счётчик, две частые операции и overflow.
 * Редкие действия (группа, шаринг, экспорт, удаление) не отнимают место у счётчика
 * на узких экранах; destructive-действие отделено разделителем.
 */
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
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            val fullTitle = pluralStringResource(R.plurals.selection_count, count, count)
            BoxWithConstraints {
                val compact = maxWidth < 96.dp || LocalDensity.current.fontScale >= 1.5f
                Text(
                    text = if (compact) count.toString() else fullTitle,
                    modifier =
                        Modifier
                            .testTag("selection_title")
                            .then(
                                if (compact) {
                                    Modifier.clearAndSetSemantics { contentDescription = fullTitle }
                                } else {
                                    Modifier
                                },
                            ),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
            Box {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Below,
                        ),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.action_more))
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("selection_more_button"),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.testTag("selection_actions_menu"),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_group)) },
                        onClick = {
                            menuExpanded = false
                            onAddToGroup()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.testTag("add_to_group_button"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.testTag("share_selected_button"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export)) },
                        onClick = {
                            menuExpanded = false
                            onExport()
                        },
                        leadingIcon = {
                            Icon(
                                BrandIcons.ImportExport,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.testTag("export_selected_button"),
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        modifier = Modifier.testTag("delete_selected_button"),
                    )
                }
            }
        },
        modifier = Modifier.testTag("selection_top_bar"),
    )
}
