package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import com.m4isper.contactsapp.R
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
) {
    val spacing = LocalSpacing.current
    var menuOpen by remember { mutableStateOf(false) }
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
                }
            }
        }
    }
}
