package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.presentation.common.ErrorState
import com.m4isper.contactsapp.presentation.common.LoadingState

internal fun groupContactsByFirstLetter(contacts: List<ContactDetails>): Map<Char, List<ContactDetails>> {
    return contacts
        .filter { it.name.isNotBlank() }
        .groupBy {
            val first = it.name.first().uppercaseChar()
            if (first.isLetter()) first else '#'
        }
        .toSortedMap()
}

@Composable
internal fun ContactList(
    uiState: ContactsUiState,
    onSearchQueryChange: (String) -> Unit,
    onContactClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text(stringResource(R.string.search_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )

        when (val content = uiState.content) {
            is ContactsContent.Loading -> LoadingState()
            is ContactsContent.Empty -> EmptyState(isSearching = content.isSearching)
            is ContactsContent.Error -> ErrorState(
                message = content.message,
                onRetry = onRetry,
            )
            is ContactsContent.Contacts -> GroupedContacts(
                contacts = content.items,
                onContactClick = onContactClick,
            )
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isSearching) {
                stringResource(R.string.contacts_nothing_found)
            } else {
                stringResource(R.string.contacts_empty)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
        )
    }
}

@Composable
private fun GroupedContacts(
    contacts: List<ContactDetails>,
    onContactClick: (Long) -> Unit,
) {
    val grouped = groupContactsByFirstLetter(contacts)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (letter, group) ->
            stickyHeader {
                Text(
                    text = letter.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
            items(
                items = group,
                key = { it.contactId },
            ) { contact ->
                ContactItem(contact = contact) { onContactClick(contact.contactId) }
            }
        }
    }
}

@Composable
private fun ContactItem(contact: ContactDetails, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = contactSubtitle(contact),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun contactSubtitle(contact: ContactDetails): String {
    val primary = contact.primaryPhone
    if (contact.phoneCount <= 1) return primary
    return "$primary · " + pluralStringResource(
        R.plurals.phone_count_extra,
        contact.phoneCount - 1,
        contact.phoneCount - 1,
    )
}
