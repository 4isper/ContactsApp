package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.BrandIcon
import com.m4isper.contactsapp.ui.theme.BrandIcons

/**
 * Верхняя панель списка в одну строку с поисковым полем (issue 62):
 * Settings и overflow сидят в той же строке, что и поиск, отдельной
 * строки с названием приложения нет. В режиме поиска поле раскрывается,
 * действия убираются, у поля появляется стрелка назад для закрытия.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(
    searchQuery: String,
    isSearchMode: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onDuplicatesClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onImportClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onExportClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    collapse: HeaderCollapseState,
    modifier: Modifier = Modifier,
    profileContactId: Long? = null,
    onProfileClick: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                // Инсет статус-бара снаружи collapse-layout: панель сжимается
                // до нуля, но сам узел стоит ниже инсета, поэтому закреплённые
                // фильтры под панелью не поднимаются в статус-бар.
                .testTag(if (isSearchMode) "home_search_row" else "home_top_bar")
                .statusBarsPadding()
                .collapsingHeader(collapse)
                .fillMaxWidth(),
    ) {
        ContactsSearchField(
            query = searchQuery,
            expanded = isSearchMode,
            onQueryChange = onSearchQueryChange,
            onExpandedChange = { shouldOpen -> if (shouldOpen) onSearchOpen() },
            onClose = onSearchClose,
            // Поле несёт свой тег "search_field"; строка — отдельный семантический узел.
            trailing = {
                if (!isSearchMode) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_action"),
                    ) {
                        BrandIcon(
                            imageVector = BrandIcons.Settings,
                            contentDescription = stringResource(R.string.settings_menu),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("more_actions_button"),
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
                                text = { Text(stringResource(R.string.recents_title)) },
                                onClick = {
                                    menuExpanded = false
                                    onRecentsClick()
                                },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                modifier = Modifier.testTag("recents_menu_item"),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.duplicates_menu)) },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicatesClick()
                                },
                                leadingIcon = { Icon(Icons.Filled.Merge, contentDescription = null) },
                                modifier = Modifier.testTag("duplicates_menu_item"),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.groups_title)) },
                                onClick = {
                                    menuExpanded = false
                                    onGroupsClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                                },
                                modifier = Modifier.testTag("groups_menu_item"),
                            )
                            if (profileContactId != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.contacts_my_profile)) },
                                    onClick = {
                                        menuExpanded = false
                                        onProfileClick()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    modifier = Modifier.testTag("profile_menu_item"),
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_import)) },
                                onClick = {
                                    menuExpanded = false
                                    onImportClick()
                                },
                                leadingIcon = { ImportExportIcon() },
                                modifier = Modifier.testTag("import_menu_item"),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_scan_qr)) },
                                onClick = {
                                    menuExpanded = false
                                    onScanQrClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                                },
                                modifier = Modifier.testTag("scan_qr_menu_item"),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export)) },
                                onClick = {
                                    menuExpanded = false
                                    onExportClick()
                                },
                                leadingIcon = { ImportExportIcon() },
                                modifier = Modifier.testTag("export_menu_item"),
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_select_all)) },
                                onClick = {
                                    menuExpanded = false
                                    onSelectAllClick()
                                },
                                leadingIcon = { Icon(Icons.Filled.SelectAll, contentDescription = null) },
                                modifier = Modifier.testTag("select_all_menu_item"),
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ImportExportIcon() {
    BrandIcon(
        imageVector = BrandIcons.ImportExport,
        contentDescription = null,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}
