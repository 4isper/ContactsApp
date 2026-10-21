package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.presentation.common.account.contactAccountLabel
import com.m4isper.contactsapp.presentation.common.account.contactAccountSubtitle
import com.m4isper.contactsapp.presentation.common.account.contactAccountTitle
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
    accounts: List<ContactAccount> = emptyList(),
    selectedAccount: ContactAccount? = null,
    onAccountSelected: (ContactAccount?) -> Unit = {},
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
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                if (accounts.isNotEmpty()) {
                    AccountFilterChip(
                        accounts = accounts,
                        selectedAccount = selectedAccount,
                        onAccountSelected = onAccountSelected,
                        modifier =
                            Modifier
                                .padding(top = spacing.xs, end = spacing.m, bottom = spacing.xs)
                                .testTag("account_chip"),
                    )
                }
            }
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
                        leadingIcon = {
                            Icon(
                                Icons.Filled.UploadFile,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.testTag("import_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export)) },
                        onClick = {
                            menuOpen = false
                            onExportClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.testTag("export_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.duplicates_menu)) },
                        onClick = {
                            menuOpen = false
                            onDuplicatesClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Merge,
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.testTag("duplicates_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_menu)) },
                        onClick = {
                            menuOpen = false
                            onSettingsClick()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = null,
                            )
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
    Box(modifier = modifier) {
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
                            Icons.AutoMirrored.Filled.Label,
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
            modifier = Modifier.widthIn(max = 220.dp),
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
                    text = {
                        Column {
                            Text(group.title)
                            Text(
                                text = contactAccountLabel(group.account),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
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

/** Чип фильтра по аккаунту: все аккаунты, локальный или конкретный синхронизируемый. */
@Composable
private fun AccountFilterChip(
    accounts: List<ContactAccount>,
    selectedAccount: ContactAccount?,
    onAccountSelected: (ContactAccount?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilterChip(
            selected = selectedAccount != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text =
                        selectedAccount?.let { contactAccountTitle(it) }
                            ?: stringResource(R.string.filter_accounts),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon =
                if (selectedAccount != null) {
                    {
                        Icon(
                            Icons.Filled.AccountCircle,
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
            modifier = Modifier.widthIn(max = 220.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_accounts_all)) },
                onClick = {
                    expanded = false
                    onAccountSelected(null)
                },
                trailingIcon = checkIcon(visible = selectedAccount == null),
                modifier = Modifier.testTag("account_all_item"),
            )
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(contactAccountTitle(account))
                            Text(
                                text = contactAccountSubtitle(account),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onAccountSelected(account)
                    },
                    trailingIcon = checkIcon(visible = selectedAccount == account),
                    modifier = Modifier.testTag("account_item_${account.testKey()}"),
                )
            }
        }
    }
}

private fun ContactAccount.testKey(): String =
    if (isLocal) {
        "local"
    } else {
        "${type.orEmpty()}_${name.orEmpty()}"
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
