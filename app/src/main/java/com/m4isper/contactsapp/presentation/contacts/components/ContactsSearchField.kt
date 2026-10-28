package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.BrandIcon
import com.m4isper.contactsapp.ui.theme.BrandIcons
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Поле поиска в стиле M3 SearchBar: свёрнуто — однострочная поверхность,
 * в фокусе — режим поиска со стрелкой назад, которая закрывает поиск.
 * [trailing] — дополнительные иконки в правой части (действия панели).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactsSearchField(
    query: String,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val spacing = LocalSpacing.current
    SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { onClose() },
        expanded = expanded,
        onExpandedChange = { isExpanded -> if (isExpanded) onExpandedChange(true) },
        placeholder = { Text(stringResource(R.string.search_label)) },
        leadingIcon = {
            if (expanded) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("search_back_button"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_close_search),
                    )
                }
            } else {
                BrandIcon(
                    imageVector = BrandIcons.Search,
                    contentDescription = null,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.action_clear_search),
                        )
                    }
                }
                trailing()
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.s)
                .testTag("search_field"),
    )
}
