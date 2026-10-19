package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.presentation.common.groups.GroupNameDialog
import com.m4isper.contactsapp.ui.components.SearchField
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun FilterHeader(
    searchQuery: String,
    showFavoritesOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFavoritesOnlyChange: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
    groups: List<ContactGroup> = emptyList(),
    selectedGroupId: Long? = null,
    onGroupSelected: (Long?) -> Unit = {},
    onCreateGroup: (String) -> Unit = {},
    onManageGroupsClick: () -> Unit = {},
    onDuplicatesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val spacing = LocalSpacing.current
    var menuOpen by remember { mutableStateOf(false) }
    var groupsMenuOpen by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    Column(modifier = modifier.fillMaxWidth()) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onFavoritesOnlyChange,
                label = { Text(stringResource(R.string.filter_favorites)) },
                leadingIcon =
                    if (showFavoritesOnly) {
                        {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .padding(horizontal = spacing.m, vertical = spacing.xs)
                        .testTag("favorites_chip"),
            )
            GroupsFilterChip(
                groups = groups,
                selectedGroup = selectedGroup,
                expanded = groupsMenuOpen,
                onExpandedChange = { groupsMenuOpen = it },
                onGroupSelected = onGroupSelected,
                onCreateGroupClick = { showNewGroupDialog = true },
                onManageGroupsClick = onManageGroupsClick,
                modifier =
                    Modifier
                        .padding(top = spacing.xs, end = spacing.m, bottom = spacing.xs)
                        .testTag("groups_chip"),
            )
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("more_actions_button"),
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_import)) },
                        onClick = {
                            menuOpen = false
                            onImportClick()
                        },
                        modifier = Modifier.testTag("import_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export)) },
                        onClick = {
                            menuOpen = false
                            onExportClick()
                        },
                        modifier = Modifier.testTag("export_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.duplicates_menu)) },
                        onClick = {
                            menuOpen = false
                            onDuplicatesClick()
                        },
                        modifier = Modifier.testTag("duplicates_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_menu)) },
                        onClick = {
                            menuOpen = false
                            onSettingsClick()
                        },
                        modifier = Modifier.testTag("settings_menu_item"),
                    )
                }
            }
        }
    }

    if (showNewGroupDialog) {
        GroupNameDialog(
            title = stringResource(R.string.groups_new),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { name ->
                showNewGroupDialog = false
                onCreateGroup(name)
            },
            onDismiss = { showNewGroupDialog = false },
            confirmTestTag = "filter_create_group_confirm",
        )
    }
}

/** Чип фильтра по группе: показывает активную группу и открывает меню выбора. */
@Composable
private fun GroupsFilterChip(
    groups: List<ContactGroup>,
    selectedGroup: ContactGroup?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGroupSelected: (Long?) -> Unit,
    onCreateGroupClick: () -> Unit,
    onManageGroupsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box {
        FilterChip(
            selected = selectedGroup != null,
            onClick = { onExpandedChange(true) },
            label = {
                Text(
                    text = selectedGroup?.title ?: stringResource(R.string.filter_groups),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon =
                if (selectedGroup != null) {
                    {
                        Icon(
                            Icons.Filled.Label,
                            contentDescription = null,
                        )
                    }
                } else {
                    null
                },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                )
            },
            modifier = modifier.widthIn(max = 220.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.groups_all)) },
                onClick = {
                    onExpandedChange(false)
                    onGroupSelected(null)
                },
                trailingIcon = checkIcon(visible = selectedGroup == null),
                modifier = Modifier.testTag("group_all_item"),
            )
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.title) },
                    onClick = {
                        onExpandedChange(false)
                        onGroupSelected(group.id)
                    },
                    trailingIcon = checkIcon(visible = group.id == selectedGroup?.id),
                    modifier = Modifier.testTag("group_item_${group.id}"),
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.groups_new)) },
                onClick = {
                    onExpandedChange(false)
                    onCreateGroupClick()
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag("group_new_item"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.groups_manage)) },
                onClick = {
                    onExpandedChange(false)
                    onManageGroupsClick()
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                    )
                },
                modifier = Modifier.testTag("group_manage_item"),
            )
        }
    }
}

@Composable
private fun checkIcon(visible: Boolean): (@Composable () -> Unit)? =
    if (visible) {
        {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
            )
        }
    } else {
        null
    }
