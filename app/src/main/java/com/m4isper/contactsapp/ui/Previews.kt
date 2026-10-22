package com.m4isper.contactsapp.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.contacts.ContactList
import com.m4isper.contactsapp.presentation.contacts.ContactsUiState
import com.m4isper.contactsapp.ui.components.ContactAvatar
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.SearchField
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import com.m4isper.contactsapp.ui.theme.LocalSpacing

private val PreviewContacts =
    listOf(
        ContactDetails(
            contactId = 1,
            name = "Anna Ivanova",
            phones = listOf(PhoneNumber("+7 900 111-11-11", PhoneKind.MOBILE)),
            photo = null,
        ),
        ContactDetails(
            contactId = 2,
            name = "Bob Smith",
            phones =
                listOf(
                    PhoneNumber("+7 900 222-22-22", PhoneKind.MOBILE),
                    PhoneNumber("+7 900 333-33-33", PhoneKind.WORK),
                ),
            photo = null,
        ),
    )

@Composable
private fun ThemedPreview(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    ContactsAppTheme(darkTheme = darkTheme, dynamicColor = false) {
        content()
    }
}

@Preview(name = "List light", showBackground = true)
@Composable
internal fun ContactListLightPreview() {
    ThemedPreview(darkTheme = false) {
        ContactListContent()
    }
}

@Preview(name = "List dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun ContactListDarkPreview() {
    ThemedPreview(darkTheme = true) {
        ContactListContent()
    }
}

@Preview(name = "List selection mode", showBackground = true)
@Composable
internal fun ContactListSelectionPreview() {
    ThemedPreview(darkTheme = false) {
        ContactListContent(selection = setOf(1, 2))
    }
}

@Composable
private fun ContactListContent(selection: Set<Long> = emptySet()) {
    ContactList(
        uiState =
            ContactsUiState(
                content = Loadable.Content(PreviewContacts),
                favorites = setOf(1),
                selection = selection,
            ),
        onSearchQueryChange = {},
        onFavoritesOnlyChange = {},
        onContactClick = {},
        onAddClick = {},
        onRetry = {},
        onRefresh = {},
        onContactSelected = {},
        onClearSelection = {},
        onSelectAll = {},
        onDeleteSelected = {},
        onUndoDelete = {},
        onDeleteWindowClosed = {},
        onShareSelected = {},
        onImportText = {},
        onExportRequested = {},
        onExportSelectionRequested = {},
        onEventHandled = {},
    )
}

@Preview(name = "Components light", showBackground = true)
@Composable
internal fun ComponentsLightPreview() {
    ThemedPreview(darkTheme = false) {
        ComponentsContent()
    }
}

@Preview(name = "Components dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun ComponentsDarkPreview() {
    ThemedPreview(darkTheme = true) {
        ComponentsContent()
    }
}

@Composable
private fun ComponentsContent() {
    Column {
        ContactAvatar(
            name = "Anna",
            photoUri = null,
            size = LocalSpacing.current.avatarList,
            textStyle = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        SearchField(value = "Ann", onValueChange = {})
        EmptyState(icon = Icons.Filled.Search, title = "Nothing found")
    }
}
